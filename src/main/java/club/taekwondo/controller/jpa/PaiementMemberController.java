package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CartCheckoutRequest;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.PaiementRequestDTO;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/paiements")
public class PaiementMemberController {

    private static final Logger log = LoggerFactory.getLogger(PaiementMemberController.class);

    private final PaiementService paiementService;
    private final PaiementAccessService paiementAccessService;

    public PaiementMemberController(PaiementService paiementService,
                                    PaiementAccessService paiementAccessService) {
        this.paiementService = paiementService;
        this.paiementAccessService = paiementAccessService;
    }

    @PreAuthorize("hasAnyRole('MEMBRE','ADMIN')")
    @GetMapping("/membre/mes-paiements")
    public ResponseEntity<List<PaiementDTO>> getPaiementsPourMembreConnecte(Authentication authentication) {
        Utilisateur compte = paiementAccessService.requireAuthenticatedUser(authentication);
        Long membreId = paiementAccessService.getAttachedMembreId(compte);

        if (membreId == null) {
            log.info("[MEMBRE] Aucun membre rattache au compte utilisateur id={} email={}", compte.getId(), compte.getEmail());
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<PaiementDTO> byMembre = paiementService.getPaiementsParMembre(membreId);
        List<PaiementDTO> byUser = paiementService.getPaiementsParUtilisateur(compte.getId());

        Map<Long, PaiementDTO> merged = new LinkedHashMap<>();
        byMembre.forEach(paiement -> merged.put(paiement.getId(), paiement));
        byUser.forEach(paiement -> merged.putIfAbsent(paiement.getId(), paiement));

        List<PaiementDTO> out = new ArrayList<>(merged.values());
        log.info("[MEMBRE] mes-paiements membreId={} (userId={}) -> {} paiements ({} par membre, {} par user)",
                membreId, compte.getId(), out.size(), byMembre.size(), byUser.size());

        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasAnyRole('MEMBRE','PARENT')")
    @PostMapping("/ajouter-membre")
    public ResponseEntity<Map<String, Object>> ajouterPaiementPourMembre(Authentication authentication,
                                                                         @RequestBody Map<String, Object> body) {
        try {
            Utilisateur utilisateur = paiementAccessService.requireAuthenticatedUser(authentication);

            Double montantTotal = PaiementRequestValues.doubleOrNull(body.get("montantTotal"));
            if (montantTotal == null || montantTotal <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "montantTotal invalide"));
            }

            String typeHuman = PaiementRequestValues.strOrNull(body.get("type"));
            String typeAlt = PaiementRequestValues.strOrNull(body.get("typePaiement"));
            String typeBack = PaiementRequestValues.normalizeTypeHuman(typeHuman != null ? typeHuman : typeAlt);

            String modeHuman = PaiementRequestValues.strOrNull(body.get("modePaiement"));
            String modeBack = PaiementRequestValues.normalizeModeHuman(modeHuman);

            Integer nombreEcheances = PaiementRequestValues.intOrNull(body.get("nombreEcheances"));
            if ("ECHELONNE".equalsIgnoreCase(typeBack)) {
                if (nombreEcheances == null || nombreEcheances < 2) {
                    nombreEcheances = 2;
                }
                if (nombreEcheances > 12) {
                    nombreEcheances = 12;
                }
            } else {
                nombreEcheances = 1;
            }

            PaiementRequestDTO req = new PaiementRequestDTO();
            req.setUtilisateurId(utilisateur.getId());
            req.setTypePaiement(typeBack);
            req.setModePaiement(modeBack);
            req.setMontantTotal(montantTotal);
            req.setNombreEcheances(nombreEcheances);
            req.setDatePaiement(LocalDate.now().toString());

            boolean isParent = utilisateur.getRole() != null
                    && "PARENT".equalsIgnoreCase(utilisateur.getRole().toString().replace("ROLE_", ""));
            if (isParent) {
                Long membreId = PaiementRequestValues.longOrNull(body.get("membreId"));
                if (membreId == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "membreId requis pour un parent"));
                }
                paiementAccessService.assertParentOwnsMember(utilisateur, membreId);
                req.setMembreId(membreId);
            } else {
                Long membreCourantId = paiementAccessService.getAttachedMembreId(utilisateur);
                if (membreCourantId != null) {
                    req.setMembreId(membreCourantId);
                    log.info("[PAY] Membre -> rattachement automatique membreId={}", membreCourantId);
                } else {
                    log.warn("[PAY] Membre connecte sans entite Membre liee (compteUtilisateurId={})", utilisateur.getId());
                }
            }

            List<PaiementDTO> created = paiementService.ajouterPaiementsCompletFromDto(req, null);
            if (created == null || created.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Aucun paiement créé"));
            }

            Long firstId = created.get(0).getId();
            Map<String, Object> response = new HashMap<>();
            response.put("paiementId", firstId);
            response.put("reference", null);
            return created("/api/paiements/" + firstId, response);
        } catch (Exception e) {
            log.error("[PAY] Erreur ajout paiement membre", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('MEMBRE','PARENT','ADMIN')")
    @PostMapping(value = "/from-cart", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> creerDepuisPanier(Authentication authentication,
                                                                @RequestBody CartCheckoutRequest req) {
        try {
            Utilisateur user = paiementAccessService.requireAuthenticatedUser(authentication);
            if (req.getItems() == null || req.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Aucun article dans le panier"));
            }

            Paiement paiement = paiementService.creerPaiementDepuisPanier(user, req);
            Map<String, Object> response = new HashMap<>();
            response.put("paiementId", paiement.getId());
            response.put("statut", paiement.getStatut());
            response.put("modePaiement", paiement.getModePaiement());
            response.put("montantTotal", paiement.getMontantTotal());
            return created("/api/paiements/" + paiement.getId(), response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création du paiement depuis le panier"));
        }
    }

    private ResponseEntity<Map<String, Object>> created(String location, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
    }
}
