package club.taekwondo.controller.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementService;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final StripeService stripeService;
    private final PaiementService paiementService;

    public PaiementController(StripeService stripeService, PaiementService paiementService) {
        this.stripeService = stripeService;
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<Paiement> getAll() {
        List<Paiement> paiements = paiementService.getAll();
        paiements.forEach(paiement -> System.out.println("Paiement renvoyé : " + paiement));
        return paiements;
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            PaymentIntent paymentIntent = stripeService.executeStripePayment(token, request);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());

            return ResponseEntity.ok(response);
        } catch (StripeException se) {
            System.out.println(se.getCode());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", se.getCode()));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur inattendue est survenue."));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Paiement>> filterPaiements(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String modePaiement) {
        List<Paiement> paiements = paiementService.filterPaiements(statut, modePaiement);
        return ResponseEntity.ok(paiements);
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<Paiement> validerPaiement(@PathVariable Long id) {
        Optional<Paiement> paiementOpt = paiementService.getById(id);
        if (paiementOpt.isPresent()) {
            Paiement paiement = paiementOpt.get();
            paiement.setStatut("payé");
            Paiement updatedPaiement = paiementService.save(paiement);
            return ResponseEntity.ok(updatedPaiement);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Paiement> annulerPaiement(@PathVariable Long id) {
        Optional<Paiement> paiementOpt = paiementService.getById(id);
        if (paiementOpt.isPresent()) {
            Paiement paiement = paiementOpt.get();
            paiement.setStatut("annulé");
            Paiement updatedPaiement = paiementService.save(paiement);
            return ResponseEntity.ok(updatedPaiement);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    @PostMapping("/{id}/payer-echeance")
    public ResponseEntity<Paiement> payerEcheance(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<Paiement> paiementOpt = paiementService.getById(id);
        if (paiementOpt.isPresent()) {
            Paiement paiement = paiementOpt.get();

            Integer nombreEcheances = (Integer) request.get("nombreEcheances");
            Double montantTotalAPayer = (Double) request.get("montantTotalAPayer");

            // Mettre à jour le montant restant et les échéances restantes
            paiement.setMontantRestant(paiement.getMontantRestant() - montantTotalAPayer);
            paiement.setEcheancesRestantes(paiement.getEcheancesRestantes() - nombreEcheances);

            // Si le montant restant est 0, marquer comme payé
            if (paiement.getMontantRestant() <= 0) {
                paiement.setStatut("payé");
                paiement.setMontantRestant(0.0); // S'assurer que le montant restant est 0
                paiement.setEcheancesRestantes(0); // Plus d'échéances restantes
            }

            Paiement updatedPaiement = paiementService.save(paiement);
            return ResponseEntity.ok(updatedPaiement);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}