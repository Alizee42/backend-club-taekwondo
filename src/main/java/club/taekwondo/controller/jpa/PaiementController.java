package club.taekwondo.controller.jpa;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import club.taekwondo.dto.DashboardStatsDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService, UtilisateurService utilisateurService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<PaiementDTO> getAll() {
        List<PaiementDTO> paiements = paiementService.getAllWithEcheances();
        paiements.forEach(p -> System.out.println(p.getEcheances()));
        return paiements;
    }

    @PostMapping("/{id}/payer-echeance")
    public ResponseEntity<Paiement> payerEcheance(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<Paiement> paiementOpt = paiementService.getById(id);
        if (paiementOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Paiement paiement = paiementOpt.get();

        System.out.println("Avant modification : " + paiement.getEcheances());

        int nombreEcheances = Integer.parseInt(request.get("nombreEcheances").toString());
        double montantTotalAPayer = Double.parseDouble(request.get("montantTotalAPayer").toString());

        if (nombreEcheances > paiement.getEcheancesRestantes()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        paiement.setMontantRestant(Math.max(0, paiement.getMontantRestant() - montantTotalAPayer));
        paiement.setEcheancesRestantes(Math.max(0, paiement.getEcheancesRestantes() - nombreEcheances));

        if (paiement.getMontantRestant() <= 0 || paiement.getEcheancesRestantes() <= 0) {
            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setEcheancesRestantes(0);
        }

        System.out.println("Après modification : " + paiement.getEcheances());

        return ResponseEntity.ok(paiementService.save(paiement));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Paiement>> filterPaiements(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String modePaiement) {
        List<Paiement> paiements = paiementService.filterPaiements(statut, modePaiement);
        paiements.forEach(p -> System.out.println(p.getEcheances()));
        return ResponseEntity.ok(paiements);
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<Paiement> validerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(paiement -> {
                    System.out.println("Avant validation : " + paiement.getEcheances());

                    paiement.setStatut("payé");
                    paiement.setEcheancesRestantes(0);
                    paiement.setMontantRestant(0.0);

                    System.out.println("Après validation : " + paiement.getEcheances());

                    return ResponseEntity.ok(paiementService.save(paiement));
                }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Paiement> annulerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(paiement -> {
                    System.out.println("Avant annulation : " + paiement.getEcheances());

                    paiement.setStatut("annulé");

                    System.out.println("Après annulation : " + paiement.getEcheances());

                    return ResponseEntity.ok(paiementService.save(paiement));
                }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/dashboard")
    public DashboardStatsDTO getDashboardStats() {
        return paiementService.buildDashboardStats();
    }
}

