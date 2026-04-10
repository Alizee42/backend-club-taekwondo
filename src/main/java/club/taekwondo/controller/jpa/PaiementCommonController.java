package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DashboardStatsDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/paiements")
public class PaiementCommonController {

    private static final Logger log = LoggerFactory.getLogger(PaiementCommonController.class);

    private final PaiementService paiementService;
    private final PaiementAccessService paiementAccessService;
    private final UtilisateurService utilisateurService;
    private final JwtUtil jwtUtil;

    public PaiementCommonController(PaiementService paiementService,
                                    PaiementAccessService paiementAccessService,
                                    UtilisateurService utilisateurService,
                                    JwtUtil jwtUtil) {
        this.paiementService = paiementService;
        this.paiementAccessService = paiementAccessService;
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<List<PaiementDTO>> getAll() {
        return ResponseEntity.ok(paiementService.getAllWithEcheances());
    }

    @GetMapping("/debug/whoami")
    public ResponseEntity<Map<String, Object>> whoami(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                      Authentication authentication) {
        Map<String, Object> out = new HashMap<>();
        try {
            out.put("securityPrincipal", authentication != null ? authentication.getPrincipal() : null);
            out.put("securityAuthorities", authentication != null
                    ? authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList()
                    : null);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                out.put("tokenRole", jwtUtil.extractRole(token));
                out.put("tokenEmail", jwtUtil.extractEmail(token));
            }

            if (authentication != null && authentication.getName() != null) {
                Optional<Utilisateur> user = utilisateurService.getUtilisateurEntityByEmail(authentication.getName());
                if (user.isPresent()) {
                    out.put("dbRole", user.get().getRole());
                    out.put("userId", user.get().getId());
                }
            }

            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(out);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    @PostMapping("/{id}/payer-echeance")
    public ResponseEntity<PaiementDTO> payerEcheance(@PathVariable Long id,
                                                     @RequestBody List<Map<String, Object>> echeancesPayees,
                                                     Authentication authentication) {
        Paiement paiement = paiementService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        paiementAccessService.assertCanAccessPaiement(authentication, paiement);

        if (paiement.getEcheances() == null || paiement.getEcheances().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        double montantTotalAPayer = 0.0;
        int nombreEcheancesPayees = 0;
        Map<Long, Echeance> echeancesById = paiement.getEcheances().stream()
                .filter(echeance -> echeance.getId() != null)
                .collect(Collectors.toMap(Echeance::getId, echeance -> echeance));

        for (Map<String, Object> payload : echeancesPayees) {
            if (payload.get("id") == null) {
                continue;
            }

            Long echeanceId = Long.parseLong(payload.get("id").toString());
            Echeance echeance = echeancesById.get(echeanceId);
            if (echeance == null) {
                return ResponseEntity.badRequest().build();
            }

            if (!"payé".equalsIgnoreCase(echeance.getStatut())) {
                echeance.setStatut("payé");
                montantTotalAPayer += echeance.getMontant();
                nombreEcheancesPayees++;
            }
        }

        paiement.setMontantPaye((paiement.getMontantPaye() == null ? 0.0 : paiement.getMontantPaye()) + montantTotalAPayer);
        double restant = (paiement.getMontantRestant() == null ? 0.0 : paiement.getMontantRestant()) - montantTotalAPayer;
        paiement.setMontantRestant(Math.max(0.0, restant));
        paiement.setEcheancesRestantes(Math.max(0,
                (paiement.getEcheancesRestantes() == null ? 0 : paiement.getEcheancesRestantes()) - nombreEcheancesPayees));

        boolean remainingUnpaid = paiement.getEcheances().stream()
                .anyMatch(echeance -> !"payé".equalsIgnoreCase(echeance.getStatut()));
        if (!remainingUnpaid) {
            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setEcheancesRestantes(0);
        } else {
            paiement.setStatut("en attente");
        }

        Paiement saved = paiementService.persisterEtat(paiement);
        return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<PaiementDTO>> filterPaiements(@RequestParam(required = false) String statut,
                                                             @RequestParam(required = false) String modePaiement) {
        List<Paiement> paiements = paiementService.filterPaiements(statut, modePaiement);
        List<PaiementDTO> dtos = paiements.stream().map(paiementService::toPaiementDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    @PostMapping("/{id}/valider")
    public ResponseEntity<PaiementDTO> validerPaiement(@PathVariable Long id, Authentication authentication) {
        Paiement paiement = paiementService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        paiementAccessService.assertCanAccessPaiement(authentication, paiement);

        try {
            Paiement saved = paiementService.validerPaiementAdmin(id);
            return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Erreur validation paiement {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(paiementService.buildDashboardStats());
    }

    @GetMapping("/{paiementId}/facture")
    public ResponseEntity<Void> telechargerFacture(@PathVariable Long paiementId) {
        try {
            URI redirect = URI.create("/api/stripe/receipt/" + paiementId);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(redirect);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
