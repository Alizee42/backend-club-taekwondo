package club.taekwondo.controller.jpa;

import java.util.*;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import club.taekwondo.dto.DashboardStatsDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.PaiementRequestDTO;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final StripeService stripeService;
    private final PaiementService paiementService;

    public PaiementController(StripeService stripeService, PaiementService paiementService, UtilisateurService utilisateurService) {
        this.stripeService = stripeService;
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<PaiementDTO> getAll() {
        List<PaiementDTO> paiements = paiementService.getAllWithEcheances();
        // Log des échéances pour chaque paiement
        paiements.forEach(p -> System.out.println(p.getEcheances()));
        return paiements;
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody PaiementRequestDTO dto) {
        try {
            PaymentIntent paymentIntent = stripeService.executeStripePayment(token, dto);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            return ResponseEntity.ok(response);

        } catch (StripeException se) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", se.getCode()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur inattendue est survenue."));
        }
    }

    @PostMapping("/{id}/payer-echeance")
    public ResponseEntity<Paiement> payerEcheance(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<Paiement> paiementOpt = paiementService.getById(id);
        if (paiementOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Paiement paiement = paiementOpt.get();

        // Log des échéances avant modification
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

        // Log des échéances après modification
        System.out.println("Après modification : " + paiement.getEcheances());

        return ResponseEntity.ok(paiementService.save(paiement));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Paiement>> filterPaiements(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String modePaiement) {
        List<Paiement> paiements = paiementService.filterPaiements(statut, modePaiement);
        // Log des échéances pour chaque paiement filtré
        paiements.forEach(p -> System.out.println(p.getEcheances()));
        return ResponseEntity.ok(paiements);
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<Paiement> validerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(paiement -> {
                    // Log des échéances avant validation
                    System.out.println("Avant validation : " + paiement.getEcheances());

                    paiement.setStatut("payé");
                    paiement.setEcheancesRestantes(0);
                    paiement.setMontantRestant(0.0);

                    // Log des échéances après validation
                    System.out.println("Après validation : " + paiement.getEcheances());

                    return ResponseEntity.ok(paiementService.save(paiement));
                }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Paiement> annulerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(paiement -> {
                    // Log des échéances avant annulation
                    System.out.println("Avant annulation : " + paiement.getEcheances());

                    paiement.setStatut("annulé");

                    // Log des échéances après annulation
                    System.out.println("Après annulation : " + paiement.getEcheances());

                    return ResponseEntity.ok(paiementService.save(paiement));
                }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/dashboard")
    public DashboardStatsDTO getDashboardStats() {
        return paiementService.buildDashboardStats(); // ✅ maintenant défini
    }


}

