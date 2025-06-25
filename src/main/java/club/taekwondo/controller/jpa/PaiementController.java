package club.taekwondo.controller.jpa;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import club.taekwondo.dto.DashboardStatsDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;

@RestController
@RequestMapping("/api/paiements")
@CrossOrigin(origins = "*") // (optionnel selon config CORS frontend)
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    // 🔹 Tous les paiements avec échéances
    @GetMapping
    public List<PaiementDTO> getAll() {
        return paiementService.getAllWithEcheances();
    }

    // 🔹 Payer une échéance (partielle ou complète)
    @PostMapping("/{id}/payer-echeance")
    public ResponseEntity<PaiementDTO> payerEcheance(@PathVariable Long id, @RequestBody List<Map<String, Object>> echeancesPayees) {
        Optional<Paiement> optPaiement = paiementService.getById(id);
        if (optPaiement.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Paiement paiement = optPaiement.get();

        double montantTotalAPayer = 0.0;
        int nombreEcheancesPayees = 0;

        for (Map<String, Object> echeanceData : echeancesPayees) {
            Long echeanceId = Long.parseLong(echeanceData.get("id").toString());
            Optional<Echeance> optEcheance = paiement.getEcheances().stream()
                    .filter(e -> e.getId().equals(echeanceId))
                    .findFirst();

            if (optEcheance.isPresent()) {
                Echeance echeance = optEcheance.get();
                if ("payé".equalsIgnoreCase(echeance.getStatut())) {
                    continue; // Échéance déjà payée
                }

                echeance.setStatut("payé");
                montantTotalAPayer += echeance.getMontant();
                nombreEcheancesPayees++;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Échéance introuvable
            }
        }

        paiement.setMontantRestant(Math.max(0, paiement.getMontantRestant() - montantTotalAPayer));
        paiement.setEcheancesRestantes(Math.max(0, paiement.getEcheancesRestantes() - nombreEcheancesPayees));

        if (paiement.getMontantRestant() <= 0 || paiement.getEcheancesRestantes() <= 0) {
            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setEcheancesRestantes(0);
        }

        Paiement saved = paiementService.save(paiement);
        return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
    }

    // 🔹 Filtrage par statut ou mode
    @GetMapping("/filter")
    public ResponseEntity<List<PaiementDTO>> filterPaiements(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String modePaiement) {
        List<Paiement> paiements = paiementService.filterPaiements(statut, modePaiement);
        List<PaiementDTO> dtos = paiements.stream().map(paiementService::toPaiementDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    // 🔹 Marquer comme payé
    @PostMapping("/{id}/valider")
    public ResponseEntity<PaiementDTO> validerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(p -> {
                    p.setStatut("payé");
                    p.setEcheancesRestantes(0);
                    p.setMontantRestant(0.0);
                    Paiement saved = paiementService.save(p);
                    return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 🔹 Marquer comme annulé
    @PostMapping("/{id}/annuler")
    public ResponseEntity<PaiementDTO> annulerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(p -> {
                    p.setStatut("annulé");
                    Paiement saved = paiementService.save(p);
                    return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 🔹 Statistiques dashboard
    @GetMapping("/dashboard")
    public DashboardStatsDTO getDashboardStats() {
        return paiementService.buildDashboardStats();
    }

    // 🔹 Ajouter un paiement manuel
    @PostMapping("/ajouter-manuel")
    public ResponseEntity<PaiementDTO> ajouterPaiementManuel(@RequestBody PaiementDTO dto) {
        try {
            Paiement paiement = paiementService.ajouterPaiementManuel(dto);
            return ResponseEntity.ok(paiementService.toPaiementDTO(paiement));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaiement(@PathVariable Long id) {
        try {
            System.out.println("Suppression du paiement avec ID : " + id);
            paiementService.delete(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.err.println("Erreur lors de la suppression du paiement : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors de la suppression du paiement : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

