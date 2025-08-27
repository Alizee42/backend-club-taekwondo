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
import java.util.NoSuchElementException;

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
     *   Echéances (ADMIN)
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

        paiement.setMontantPaye((paiement.getMontantPaye() == null ? 0.0 : paiement.getMontantPaye()) + montantTotalAPayer);
        double restant = (paiement.getMontantRestant() == null ? 0.0 : paiement.getMontantRestant()) - montantTotalAPayer;
        paiement.setMontantRestant(Math.max(0.0, restant));
        paiement.setEcheancesRestantes(Math.max(0, (paiement.getEcheancesRestantes() == null ? 0 : paiement.getEcheancesRestantes()) - nombreEcheancesPayees));

        boolean resteNonPaye = paiement.getEcheances().stream().anyMatch(e -> !"payé".equalsIgnoreCase(e.getStatut()));
        if (!resteNonPaye) {
            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setEcheancesRestantes(0);
        } else {
            paiement.setStatut("en attente");
        }

        Paiement saved = paiementService.persisterEtat(paiement);
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
        try {
            Paiement saved = paiementService.validerPaiementAdmin(id);
            return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/ajouter-manuel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ajouterPaiementManuel(@RequestBody Map<String, Object> body) {
        try {
            PaiementRequestDTO req = new PaiementRequestDTO();

            Long utilisateurId = longOrNull(body.get("utilisateurId"));
            Long parentId = longOrNull(body.get("parentId"));
            req.setUtilisateurId(utilisateurId != null ? utilisateurId : parentId);

            Long membreId = longOrNull(body.get("membreId"));
            req.setMembreId(membreId);
            List<Long> membreIds = listOfLong(body.get("membreIds"));
            req.setMembreIds((membreIds != null && !membreIds.isEmpty()) ? membreIds : null);

            String typeHuman = strOrNull(body.get("type"));
            String typeAlt = strOrNull(body.get("typePaiement"));
            String typeBack = normalizeTypeHuman(typeHuman != null ? typeHuman : typeAlt);
            req.setTypePaiement(typeBack);

            String modeHuman = strOrNull(body.get("modePaiement"));
            req.setModePaiement(normalizeModeHuman(modeHuman));

            String datePaiement = strOrNull(body.get("datePaiement"));
            req.setDatePaiement(datePaiement != null ? datePaiement : LocalDate.now().toString());

            Double montantTotal = doubleOrNull(body.get("montantTotal"));
            req.setMontantTotal(montantTotal);

            req.setCommentaire(strOrNull(body.get("commentaire")));

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

            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, null);

            if (created == null || created.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Aucun paiement créé"));
            }
            Long firstId = created.get(0).getId();
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
            PaiementRequestDTO req = new PaiementRequestDTO();
            req.setUtilisateurNom(utilisateurNom);
            req.setUtilisateurPrenom(utilisateurPrenom);
            req.setUtilisateurEmail(utilisateurEmail);
            req.setTypePaiement(normalizeTypeHuman(typeHuman));
            req.setModePaiement(normalizeModeHuman(modeHuman));
            req.setDatePaiement((datePaiement != null && !datePaiement.isBlank())
                    ? datePaiement
                    : LocalDate.now().toString());
            req.setMontantTotal(Double.valueOf(montantTotalStr));

            if (echeancesJson != null && !echeancesJson.isBlank()) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<PaiementRequestDTO.EcheanceInput> echs =
                        mapper.readValue(echeancesJson, mapper.getTypeFactory()
                                .constructCollectionType(List.class, PaiementRequestDTO.EcheanceInput.class));
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

    @PreAuthorize("hasAnyAuthority('PARENT','ADMIN')")
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

    @PreAuthorize("hasAnyAuthority('PARENT','ADMIN')")
    @PostMapping("/parent/ajouter")
    public ResponseEntity<PaiementDTO> ajouterPaiementParent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(token);

            Utilisateur parent = utilisateurService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Parent non trouvé"));

            PaiementRequestDTO req = new PaiementRequestDTO();

            Long membreId = longOrNull(body.get("membreId"));
            if (membreId == null || membreId <= 0) {
                return ResponseEntity.badRequest().build();
            }
            req.setMembreId(membreId);

            String typeHuman = strOrNull(body.get("type"));
            String typeAlt   = strOrNull(body.get("typePaiement"));
            req.setTypePaiement(normalizeTypeHuman(typeHuman != null ? typeHuman : typeAlt));

            String modeHuman = strOrNull(body.get("modePaiement"));
            req.setModePaiement(normalizeModeHuman(modeHuman));

            Double montantTotal = doubleOrNull(body.get("montantTotal"));
            req.setMontantTotal(montantTotal);

            Integer nbEch = intOrNull(body.get("nombreEcheances"));
            req.setNombreEcheances(nbEch);

            req.setDatePaiement(LocalDate.now().toString());

            Paiement paiement = paiementService.ajouterPaiementParent(req, parent.getId());

            PaiementDTO out = paiementService.toPaiementDTO(paiement);
            return createdDTO("/api/paiements/" + out.getId(), out);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ===========================
     *   Espace Membre (SÉCURISÉ)
     * =========================== */

    /**
     * Route dédiée aux MEMBRE / PARENT.
     * - MEMBRE : on ne passe PAS par des setters d'entité; on construit un DTO et on délègue au service.
     *            On ne requiert pas membreId (on rattache par utilisateurId).
     * - PARENT : membreId requis et on vérifie qu'il s'agit bien d'un de ses enfants.
     * Body attendu (JSON minimal) :
     * { "montantTotal": 300, "type": "UNIQUE|ECHELONNE", "modePaiement": "CB|VIREMENT|CHEQUE|ESPECES", "nombreEcheances": 3, "membreId": <enfant si parent> }
     */
    @PreAuthorize("hasAnyRole('MEMBRE','PARENT')")
    @PostMapping("/ajouter-membre")
    public ResponseEntity<Map<String, Object>> ajouterPaiementPourMembre(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token manquant ou invalide"));
            }
            final String jwt = authHeader.substring(7);
            final String email = jwtUtil.extractEmail(jwt);

            Utilisateur utilisateur = utilisateurService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // normalisations
            Double montantTotal = doubleOrNull(body.get("montantTotal"));
            if (montantTotal == null || montantTotal <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "montantTotal invalide"));
            }
            String typeHuman = strOrNull(body.get("type"));
            String typeAlt   = strOrNull(body.get("typePaiement"));
            String typeBack  = normalizeTypeHuman(typeHuman != null ? typeHuman : typeAlt);

            String modeHuman = strOrNull(body.get("modePaiement"));
            String modeBack  = normalizeModeHuman(modeHuman);

            Integer nbEch = intOrNull(body.get("nombreEcheances"));
            if ("ECHELONNE".equalsIgnoreCase(typeBack)) {
                if (nbEch == null || nbEch < 2) nbEch = 2;
                if (nbEch > 12) nbEch = 12;
            } else {
                nbEch = 1;
            }

            // Construire un DTO et laisser le service créer l'entité (évite les setters manquants)
            PaiementRequestDTO req = new PaiementRequestDTO();
            req.setUtilisateurId(utilisateur.getId());
            req.setTypePaiement(typeBack);
            req.setModePaiement(modeBack);
            req.setMontantTotal(montantTotal);
            req.setNombreEcheances(nbEch);
            req.setDatePaiement(LocalDate.now().toString());

            // Si PARENT → membreId obligatoire + vérification appartenance
            boolean isParent = utilisateur.getRole() != null &&
                    ("PARENT".equalsIgnoreCase(utilisateur.getRole().toString()) ||
                     "ROLE_PARENT".equalsIgnoreCase(utilisateur.getRole().toString()));
            if (isParent) {
                Long membreId = longOrNull(body.get("membreId"));
                if (membreId == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "membreId requis pour un parent"));
                }
                List<Membre> enfants = membreService.getEnfantsDuParent(utilisateur.getId());
                boolean autorise = enfants.stream().anyMatch(m -> Objects.equals(m.getId(), membreId));
                if (!autorise) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Ce membre ne vous est pas rattaché"));
                }
                req.setMembreId(membreId);
            } else {
                // MEMBRE: idéalement rattacher au membre du compte (si ton service sait le faire via utilisateurId)
                // On ne met PAS de membreId pour éviter d'exiger un service backend spécifique ici.
            }

            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, null);
            if (created == null || created.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Aucun paiement créé"));
            }
            Long firstId = created.get(0).getId();
            Map<String, Object> resp = new HashMap<>();
            resp.put("paiementId", firstId);
            resp.put("reference", null);

            return created("/api/paiements/" + firstId, resp);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
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

    private ResponseEntity<PaiementDTO> createdDTO(String location, PaiementDTO body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
    }

    // ----- helpers parse / normalisation -----
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

    /** 'unique' | 'échelonné' | 'echeances' → 'UNIQUE' | 'ECHELONNE' */
    private static String normalizeTypeHuman(String t) {
        if (t == null) return "UNIQUE";
        t = t.toLowerCase(Locale.ROOT).replace("é", "e").trim();
        if (t.startsWith("echel")) return "ECHELONNE";
        if (t.equals("echeances")) return "ECHELONNE";
        return "UNIQUE";
    }

    /** 'stripe' | 'cb' | 'carte' → 'CB' ; 'virement' → 'VIREMENT' ; 'cheque' → 'CHEQUE' ; sinon 'ESPECES' */
    private static String normalizeModeHuman(String m) {
        if (m == null) return "ESPECES";
        m = m.toLowerCase(Locale.ROOT).replace("é", "e").trim();
        if (m.equals("stripe") || m.equals("cb") || m.equals("carte") || m.equals("carte bancaire")) return "CB";
        if (m.equals("virement")) return "VIREMENT";
        if (m.equals("cheque")) return "CHEQUE";
        return "ESPECES";
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}/annuler")
    public ResponseEntity<PaiementDTO> annulerPaiement(
            @PathVariable Long id,
            @RequestBody AnnulationRequestDTO dto) {
        PaiementDTO out = paiementService.annulerPaiement(id, dto);
        return ResponseEntity.ok(out);
    }
}

