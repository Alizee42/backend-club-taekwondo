package club.taekwondo.controller.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/paiements")
@CrossOrigin(origins = "*")
public class PaiementController {

    private final PaiementService paiementService;
    private final UtilisateurService utilisateurService;
    private final MembreService membreService;
    private final JwtUtil jwtUtil;

    public PaiementController(
            PaiementService paiementService,
            UtilisateurService utilisateurService,
            MembreService membreService,
            JwtUtil jwtUtil
    ) {
        this.paiementService = paiementService;
        this.utilisateurService = utilisateurService;
        this.membreService = membreService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<List<PaiementDTO>> getAll() {
        return ResponseEntity.ok(paiementService.getAllWithEcheances());
    }

    /* ===========================
     *   Echéances
     * =========================== */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/payer-echeance")
    public ResponseEntity<PaiementDTO> payerEcheance(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> echeancesPayees) {

        Optional<Paiement> optPaiement = paiementService.getById(id);
        if (optPaiement.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Paiement paiement = optPaiement.get();
        if (paiement.getEcheances() == null || paiement.getEcheances().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        double montantTotalAPayer = 0.0;
        int nombreEcheancesPayees = 0;

        Map<Long, Echeance> index = paiement.getEcheances().stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(Echeance::getId, e -> e));

        for (Map<String, Object> map : echeancesPayees) {
            if (map.get("id") == null) continue;
            Long echeanceId = Long.parseLong(map.get("id").toString());
            Echeance e = index.get(echeanceId);
            if (e == null) return ResponseEntity.badRequest().build();

            if (!"payé".equalsIgnoreCase(e.getStatut())) {
                e.setStatut("payé");
                montantTotalAPayer += e.getMontant();
                nombreEcheancesPayees++;
            }
        }

        // Recalcule agrégats
        paiement.setMontantPaye((paiement.getMontantPaye() == null ? 0.0 : paiement.getMontantPaye()) + montantTotalAPayer);
        double restant = (paiement.getMontantRestant() == null ? 0.0 : paiement.getMontantRestant()) - montantTotalAPayer;
        paiement.setMontantRestant(Math.max(0.0, restant));
        paiement.setEcheancesRestantes(Math.max(0, (paiement.getEcheancesRestantes() == null ? 0 : paiement.getEcheancesRestantes()) - nombreEcheancesPayees));

        // Statut global
        boolean resteNonPaye = paiement.getEcheances().stream().anyMatch(e -> !"payé".equalsIgnoreCase(e.getStatut()));
        if (!resteNonPaye) {
            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setEcheancesRestantes(0);
        } else {
            paiement.setStatut("en attente");
        }

        Paiement saved = paiementService.save(paiement);
        return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
    }

    /* ===========================
     *   Filtres & validations
     * =========================== */
    @GetMapping("/filter")
    public ResponseEntity<List<PaiementDTO>> filterPaiements(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String modePaiement) {
        List<Paiement> paiements = paiementService.filterPaiements(statut, modePaiement);
        List<PaiementDTO> dtos = paiements.stream().map(paiementService::toPaiementDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/valider")
    public ResponseEntity<PaiementDTO> validerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(p -> {
                    p.setStatut("payé");
                    p.setEcheancesRestantes(0);
                    p.setMontantRestant(0.0);
                    p.setMontantPaye(p.getMontantTotal() != null ? p.getMontantTotal() : 0.0);
                    Paiement saved = paiementService.save(p);
                    return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /* ===========================
     *   Dashboard
     * =========================== */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(paiementService.buildDashboardStats());
    }

    /* ===========================
     *   Création (ADMIN)
     * =========================== */

    /**
     * JSON (utilisateur/membre existant).
     * Tolère 'parentId' et 'membreIds' envoyés par le front.
     * Retourne { paiementId, reference? } pour matcher PaymentAdminService côté Angular.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/ajouter-manuel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ajouterPaiementManuel(@RequestBody Map<String, Object> body) {
        try {
            // ---- Mapping flexible du JSON reçu → PaiementRequestDTO ----
            PaiementRequestDTO req = new PaiementRequestDTO();

            // utilisateurId ou parentId
            Long utilisateurId = longOrNull(body.get("utilisateurId"));
            Long parentId = longOrNull(body.get("parentId"));
            req.setUtilisateurId(utilisateurId != null ? utilisateurId : parentId);

            // membreId / membreIds
            Long membreId = longOrNull(body.get("membreId"));
            req.setMembreId(membreId);
            List<Long> membreIds = listOfLong(body.get("membreIds"));
            req.setMembreIds((membreIds != null && !membreIds.isEmpty()) ? membreIds : null);

            // type / typePaiement (front: 'unique' | 'échelonné')
            String typeHuman = strOrNull(body.get("type"));
            String typeAlt = strOrNull(body.get("typePaiement"));
            String typeBack = PaiementService.normalizeTypeHuman(typeHuman != null ? typeHuman : typeAlt);
            req.setTypePaiement(typeBack);

            // modePaiement (front: 'especes' | 'virement' | 'stripe')
            String modeHuman = strOrNull(body.get("modePaiement"));
            req.setModePaiement(PaiementService.normalizeModeHuman(modeHuman));

            // datePaiement
            String datePaiement = strOrNull(body.get("datePaiement"));
            req.setDatePaiement(datePaiement != null ? datePaiement : LocalDate.now().toString());

            // montantTotal
            Double montantTotal = doubleOrNull(body.get("montantTotal"));
            req.setMontantTotal(montantTotal);

            // commentaire (facultatif)
            req.setCommentaire(strOrNull(body.get("commentaire")));

            // échéances (facultatif)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> echs = (List<Map<String, Object>>) body.get("echeances");
            if (echs != null && !echs.isEmpty()) {
                List<PaiementRequestDTO.EcheanceInput> list = new ArrayList<>();
                int i = 1;
                for (Map<String, Object> e : echs) {
                    PaiementRequestDTO.EcheanceInput ei = new PaiementRequestDTO.EcheanceInput();
                    ei.setNumero(intOrNull(e.get("numero")) != null ? intOrNull(e.get("numero")) : i++);
                    ei.setDateEcheance(strOrNull(e.get("dateEcheance")));
                    ei.setMontant(doubleOrNull(e.get("montant")));
                    String st = strOrNull(e.get("statut"));
                    ei.setStatut(st != null ? st : "en attente");
                    list.add(ei);
                }
                req.setEcheances(list);
            }

            // ---- Appel service ----
            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, null);

            if (created == null || created.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Aucun paiement créé"));
            }
            Long firstId = created.get(0).getId();
            // Réponse compacte attendue par l’Angular service (PaiementResponse)
            Map<String, Object> resp = new HashMap<>();
            resp.put("paiementId", firstId);
            resp.put("reference", null);

            return created("/api/paiements/" + firstId, resp);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l’ajout du paiement"));
        }
    }

    /**
     * JSON complet (création à la volée possible, sans fichier).
     * Retourne { paiementId, reference? }.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/ajouter-complet", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ajouterPaiementCompletJson(@Valid @RequestBody PaiementRequestDTO req) {
        try {
            if (req.getDatePaiement() == null || req.getDatePaiement().isBlank()) {
                req.setDatePaiement(LocalDate.now().toString());
            }
            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, null);
            Long firstId = (created != null && !created.isEmpty()) ? created.get(0).getId() : null;

            Map<String, Object> resp = new HashMap<>();
            resp.put("paiementId", firstId);
            resp.put("reference", null);

            return created(firstId != null ? ("/api/paiements/" + firstId) : "/api/paiements", resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l’ajout du paiement"));
        }
    }

    /**
     * MULTIPART (création à la volée avec justificatif) :
     * Clés attendues :
     * - utilisateurNom, utilisateurPrenom, utilisateurEmail? (optionnel)
     * - type ('unique' | 'échelonné')
     * - montantTotal
     * - modePaiement ('especes' | 'virement' | 'stripe')
     * - datePaiement (yyyy-MM-dd)
     * - echeances (JSON string) ex: [{"dateEcheance":"2025-09-01","montant":100,"statut":"en attente","numero":1}]
     * - justificatif (fichier) optionnel
     *
     * Retourne { paiementId, reference? }.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/ajouter-complet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ajouterPaiementCompletMultipart(
            @RequestPart("utilisateurNom") String utilisateurNom,
            @RequestPart("utilisateurPrenom") String utilisateurPrenom,
            @RequestPart(value = "utilisateurEmail", required = false) String utilisateurEmail,
            @RequestPart("type") String typeHuman,
            @RequestPart("montantTotal") String montantTotalStr,
            @RequestPart("modePaiement") String modeHuman,
            @RequestPart("datePaiement") String datePaiement,
            @RequestPart(value = "echeances", required = false) String echeancesJson,
            @RequestPart(value = "justificatif", required = false) MultipartFile justificatif
    ) {
        try {
            // Build DTO
            PaiementRequestDTO req = new PaiementRequestDTO();
            req.setUtilisateurNom(utilisateurNom);
            req.setUtilisateurPrenom(utilisateurPrenom);
            req.setUtilisateurEmail(utilisateurEmail);
            req.setTypePaiement(PaiementService.normalizeTypeHuman(typeHuman));
            req.setModePaiement(PaiementService.normalizeModeHuman(modeHuman));
            req.setDatePaiement((datePaiement != null && !datePaiement.isBlank())
                    ? datePaiement
                    : LocalDate.now().toString());
            req.setMontantTotal(Double.valueOf(montantTotalStr));

            if (echeancesJson != null && !echeancesJson.isBlank()) {
                List<PaiementRequestDTO.EcheanceInput> echs = new ObjectMapper().readValue(
                        echeancesJson,
                        new TypeReference<List<PaiementRequestDTO.EcheanceInput>>() {}
                );
                req.setEcheances(echs);
            }

            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, justificatif);
            Long firstId = (created != null && !created.isEmpty()) ? created.get(0).getId() : null;

            Map<String, Object> resp = new HashMap<>();
            resp.put("paiementId", firstId);
            resp.put("reference", null);

            return created(firstId != null ? ("/api/paiements/" + firstId) : "/api/paiements", resp);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l’ajout du paiement"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaiement(@PathVariable Long id) {
        try {
            paiementService.delete(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ===========================
     *   Espace Parent
     * =========================== */

    /** Récupère uniquement les paiements des enfants du parent connecté */
    @GetMapping("/parent/mes-paiements")
    public ResponseEntity<List<PaiementDTO>> getPaiementsPourParentConnecte(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);

        Utilisateur parent = utilisateurService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Parent non trouvé"));

        List<Membre> enfants = membreService.getEnfantsDuParent(parent.getId());
        List<Long> enfantsIds = enfants.stream().map(Membre::getId).toList();

        List<PaiementDTO> paiements = paiementService.getPaiementsParMembres(enfantsIds);
        return ResponseEntity.ok(paiements);
    }

    @PostMapping("/parent/ajouter")
    public ResponseEntity<PaiementDTO> ajouterPaiementParent(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody PaiementDTO dto) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(token);

            Utilisateur parent = utilisateurService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Parent non trouvé"));

            if (dto.getMembreId() == null || dto.getMembreId() <= 0) {
                return ResponseEntity.badRequest().build();
            }

            Paiement paiement = paiementService.ajouterPaiementParent(dto, parent.getId());
            PaiementDTO out = paiementService.toPaiementDTO(paiement);
            return created("/api/paiements/" + out.getId(), out);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ===========================
     *  Helpers
     * =========================== */
    private ResponseEntity<Map<String, Object>> created(String location, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
    }

    // ----- helpers parse souples -----
    private static String strOrNull(Object o) { return o == null ? null : String.valueOf(o); }
    private static Long longOrNull(Object o) {
        if (o == null) return null;
        try { return Long.valueOf(String.valueOf(o)); } catch (Exception e) { return null; }
    }
    private static Integer intOrNull(Object o) {
        if (o == null) return null;
        try { return Integer.valueOf(String.valueOf(o)); } catch (Exception e) { return null; }
    }
    private static Double doubleOrNull(Object o) {
        if (o == null) return null;
        try { return Double.valueOf(String.valueOf(o)); } catch (Exception e) { return null; }
    }
    @SuppressWarnings("unchecked")
    private static List<Long> listOfLong(Object o) {
        if (o == null) return null;
        try {
            if (o instanceof List<?> raw) {
                List<Long> out = new ArrayList<>();
                for (Object el : raw) {
                    Long v = longOrNull(el);
                    if (v != null) out.add(v);
                }
                return out;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

