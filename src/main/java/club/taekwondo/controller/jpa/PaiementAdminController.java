package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AnnulationRequestDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.PaiementRequestDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
public class PaiementAdminController {

    private final PaiementService paiementService;
    private final PaiementAccessService paiementAccessService;
    private final ObjectMapper objectMapper;

    public PaiementAdminController(PaiementService paiementService,
                                   PaiementAccessService paiementAccessService,
                                   ObjectMapper objectMapper) {
        this.paiementService = paiementService;
        this.paiementAccessService = paiementAccessService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PaiementDTO>> getPaiementsByClub(@PathVariable Long clubId,
                                                                Authentication authentication) {
        if (!paiementAccessService.hasAnyRole(authentication, "SUPER_ADMIN")) {
            Utilisateur user = paiementAccessService.requireAuthenticatedUser(authentication);
            if (user.getClub() == null || !user.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(paiementService.getPaiementsByClubId(clubId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/ajouter-manuel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ajouterPaiementManuel(@RequestBody Map<String, Object> body) {
        try {
            PaiementRequestDTO req = new PaiementRequestDTO();

            Long utilisateurId = PaiementRequestValues.longOrNull(body.get("utilisateurId"));
            Long parentId = PaiementRequestValues.longOrNull(body.get("parentId"));
            req.setUtilisateurId(utilisateurId != null ? utilisateurId : parentId);

            Long membreId = PaiementRequestValues.longOrNull(body.get("membreId"));
            req.setMembreId(membreId);
            List<Long> membreIds = PaiementRequestValues.listOfLong(body.get("membreIds"));
            req.setMembreIds((membreIds != null && !membreIds.isEmpty()) ? membreIds : null);

            String typeHuman = PaiementRequestValues.strOrNull(body.get("type"));
            String typeAlt = PaiementRequestValues.strOrNull(body.get("typePaiement"));
            req.setTypePaiement(PaiementRequestValues.normalizeTypeHuman(typeHuman != null ? typeHuman : typeAlt));

            String modeHuman = PaiementRequestValues.strOrNull(body.get("modePaiement"));
            req.setModePaiement(PaiementRequestValues.normalizeModeHuman(modeHuman));

            String datePaiement = PaiementRequestValues.strOrNull(body.get("datePaiement"));
            req.setDatePaiement(datePaiement != null ? datePaiement : LocalDate.now().toString());

            req.setMontantTotal(PaiementRequestValues.doubleOrNull(body.get("montantTotal")));
            req.setCommentaire(PaiementRequestValues.strOrNull(body.get("commentaire")));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> echeances = (List<Map<String, Object>>) body.get("echeances");
            if (echeances != null && !echeances.isEmpty()) {
                List<PaiementRequestDTO.EcheanceInput> items = new ArrayList<>();
                int fallbackNumero = 1;
                for (Map<String, Object> echeance : echeances) {
                    PaiementRequestDTO.EcheanceInput input = new PaiementRequestDTO.EcheanceInput();
                    Integer numero = PaiementRequestValues.intOrNull(echeance.get("numero"));
                    input.setNumero(numero != null ? numero : fallbackNumero++);
                    input.setDateEcheance(PaiementRequestValues.strOrNull(echeance.get("dateEcheance")));
                    input.setMontant(PaiementRequestValues.doubleOrNull(echeance.get("montant")));
                    String statut = PaiementRequestValues.strOrNull(echeance.get("statut"));
                    input.setStatut(statut != null ? statut : "en attente");
                    items.add(input);
                }
                req.setEcheances(items);
            }

            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, null);
            if (created == null || created.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Aucun paiement créé"));
            }

            Long firstId = created.get(0).getId();
            Map<String, Object> response = new HashMap<>();
            response.put("paiementId", firstId);
            response.put("reference", null);
            return created("/api/paiements/" + firstId, response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'ajout du paiement"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/ajouter-complet", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ajouterPaiementCompletJson(@Valid @RequestBody PaiementRequestDTO req) {
        try {
            if (req.getDatePaiement() == null || req.getDatePaiement().isBlank()) {
                req.setDatePaiement(LocalDate.now().toString());
            }

            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, null);
            Long firstId = (created != null && !created.isEmpty()) ? created.get(0).getId() : null;

            Map<String, Object> response = new HashMap<>();
            response.put("paiementId", firstId);
            response.put("reference", null);
            return created(firstId != null ? "/api/paiements/" + firstId : "/api/paiements", response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'ajout du paiement"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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
            @RequestPart(value = "justificatif", required = false) MultipartFile justificatif) {
        try {
            PaiementRequestDTO req = new PaiementRequestDTO();
            req.setUtilisateurNom(utilisateurNom);
            req.setUtilisateurPrenom(utilisateurPrenom);
            req.setUtilisateurEmail(utilisateurEmail);
            req.setTypePaiement(PaiementRequestValues.normalizeTypeHuman(typeHuman));
            req.setModePaiement(PaiementRequestValues.normalizeModeHuman(modeHuman));
            req.setDatePaiement((datePaiement != null && !datePaiement.isBlank())
                    ? datePaiement
                    : LocalDate.now().toString());
            req.setMontantTotal(Double.valueOf(montantTotalStr));

            if (echeancesJson != null && !echeancesJson.isBlank()) {
                List<PaiementRequestDTO.EcheanceInput> echeances = objectMapper.readValue(
                        echeancesJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PaiementRequestDTO.EcheanceInput.class)
                );
                req.setEcheances(echeances);
            }

            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, justificatif);
            Long firstId = (created != null && !created.isEmpty()) ? created.get(0).getId() : null;

            Map<String, Object> response = new HashMap<>();
            response.put("paiementId", firstId);
            response.put("reference", null);
            return created(firstId != null ? "/api/paiements/" + firstId : "/api/paiements", response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'ajout du paiement"));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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

    @PostMapping("/admin/backfill-club")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> backfillClub() {
        return ResponseEntity.ok(Map.of("updated", paiementService.backfillClubForExistingPaiements()));
    }

    @PostMapping("/admin/backfill-charge")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> backfillCharge() {
        return ResponseEntity.ok(Map.of("updated", paiementService.backfillStripeChargeInfoForExistingPaiements()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}/annuler")
    public ResponseEntity<PaiementDTO> annulerPaiement(@PathVariable Long id,
                                                       @RequestBody AnnulationRequestDTO dto) {
        return ResponseEntity.ok(paiementService.annulerPaiement(id, dto));
    }

    private ResponseEntity<Map<String, Object>> created(String location, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
    }
}
