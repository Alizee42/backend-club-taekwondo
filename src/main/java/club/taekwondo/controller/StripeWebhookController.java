package club.taekwondo.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PaiementService paiementService;

    public StripeWebhookController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @PostConstruct
    public void checkWebhookSecret() {
        if (endpointSecret == null || endpointSecret.isEmpty()) {
            System.err.println("⚠️ webhook.secret Stripe non configuré !");
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("❌ Signature Stripe invalide : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        }

        System.out.println("📦 Stripe Mode : " + (event.getLivemode() ? "LIVE" : "TEST"));
        System.out.println("ℹ️ Type d'événement Stripe reçu : " + event.getType());

        if ("payment_intent.succeeded".equals(event.getType())) {
            System.out.println("✅ Paiement réussi détecté");

            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (paymentIntent != null) {
                String paiementIdStr = paymentIntent.getMetadata().get("paiementId");
                System.out.println("📌 Metadata : " + paymentIntent.getMetadata());

                if (paiementIdStr != null) {
                    try {
                        Long paiementId = Long.parseLong(paiementIdStr);
                        Optional<Paiement> optional = paiementService.getById(paiementId);
                        if (optional.isPresent()) {
                            Paiement paiement = optional.get();

                            if (!"payé".equalsIgnoreCase(paiement.getStatut())) {
                                paiement.setStatut("payé");
                                paiementService.save(paiement);
                                System.out.printf("✅ Paiement confirmé : ID=%d, Montant=%.2f, UtilisateurID=%d%n",
                                        paiement.getId(), paiement.getMontantTotal(), paiement.getUtilisateur().getId());
                            } else {
                                System.out.println("⚠️ Paiement déjà marqué comme payé.");
                            }

                        } else {
                            System.err.println("❌ Paiement non trouvé pour ID : " + paiementId);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("❌ paiementId invalide dans metadata : " + paiementIdStr);
                    }
                } else {
                    System.err.println("⚠️ paiementId manquant dans metadata Stripe");
                }
            }
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            System.out.println("❌ Paiement échoué Stripe : " + event.getId());
        }

        return ResponseEntity.ok("Webhook reçu");
    }
}
