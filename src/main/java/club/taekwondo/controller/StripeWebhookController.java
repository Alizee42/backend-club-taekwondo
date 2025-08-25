package club.taekwondo.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.Map;
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
            System.err.println("⚠️ stripe.webhook.secret non configuré !");
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
        System.out.println("ℹ️ Événement Stripe : " + event.getType());

        var dataObj = event.getDataObjectDeserializer().getObject();
        if (dataObj.isEmpty()) {
            System.err.println("⚠️ Impossible de désérialiser l'objet Stripe.");
            return ResponseEntity.ok("ignored");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent pi = (PaymentIntent) dataObj.get();
            Map<String, String> md = pi.getMetadata();
            System.out.println("📌 Metadata PI: " + md);

            String paiementIdStr = md.get("paiementId");
            if (paiementIdStr == null || paiementIdStr.isBlank()) {
                System.err.println("⚠️ paiementId manquant dans metadata Stripe (aucune mise à jour BDD).");
                return ResponseEntity.ok("no-paiementId");
            }

            Long paiementId;
            try {
                paiementId = Long.parseLong(paiementIdStr);
            } catch (NumberFormatException nfe) {
                System.err.println("❌ paiementId invalide: " + paiementIdStr);
                return ResponseEntity.ok("invalid-paiementId");
            }

            Optional<Paiement> optional = paiementService.getById(paiementId);
            if (optional.isEmpty()) {
                System.err.println("❌ Paiement non trouvé pour ID: " + paiementId);
                return ResponseEntity.ok("paiement-not-found");
            }

            Paiement paiement = optional.get();

            // Idempotence : si déjà payé, on ne refait rien
            if ("payé".equalsIgnoreCase(paiement.getStatut())) {
                System.out.println("ℹ️ Paiement déjà marqué comme payé, on ignore.");
                return ResponseEntity.ok("already-paid");
            }

            // (Optionnel) IDs Stripe si colonnes dispo
         // (Optionnel) IDs Stripe pour debug / traçabilité (pas stockés en BDD ici)
            String intentId = pi.getId();
            String chargeId = (pi.getLatestChargeObject() != null) ? pi.getLatestChargeObject().getId() : null;
            System.out.println("[Stripe] intentId=" + intentId + " | chargeId=" + chargeId);


            // ✅ MAJ mode + statut
            paiement.setModePaiement("CB");
            paiement.setStatut("payé");

            // ✅ Cas ÉCHÉLONNÉ : solder la 1ʳᵉ échéance non payée + recalculer
            if ("ECHELONNE".equalsIgnoreCase(paiement.getType())
                    && paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {

                paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .sorted(Comparator.comparingInt(Echeance::getNumero))
                        .findFirst()
                        .ifPresent(e -> e.setStatut("payé"));

                double restant = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .mapToDouble(Echeance::getMontant) // ← plus de comparaison à null
                        .sum();

                long nbRestantes = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .count();

                paiement.setMontantRestant(restant);
                paiement.setEcheancesRestantes((int) nbRestantes);
            } else {
                // Paiement unique : rien de plus à faire
                paiement.setMontantRestant(0.0);
                paiement.setEcheancesRestantes(0);
            }

            paiementService.save(paiement);
            System.out.printf("✅ Paiement confirmé: ID=%d, Montant=%.2f, Mode=%s%n",
                    paiement.getId(), paiement.getMontantTotal(), paiement.getModePaiement());

        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent pi = (PaymentIntent) dataObj.get();
            Map<String, String> md = pi.getMetadata();
            String paiementIdStr = md.get("paiementId");
            String reason = (pi.getLastPaymentError() != null) ? pi.getLastPaymentError().getMessage() : "unknown";

            if (paiementIdStr != null && !paiementIdStr.isBlank()) {
                try {
                    Long paiementId = Long.parseLong(paiementIdStr);
                    Optional<Paiement> optional = paiementService.getById(paiementId);
                    if (optional.isPresent()) {
                        System.out.println("❌ Paiement échoué (paiementId=" + paiementId + ") : " + reason);
                        // on laisse "en attente" pour permettre un nouvel essai
                    } else {
                        System.out.println("❌ Paiement échoué mais paiementId inexistant en BDD : " + paiementId);
                    }
                } catch (NumberFormatException ignored) {
                    System.out.println("❌ payment_failed: paiementId invalide dans metadata: " + paiementIdStr);
                }
            } else {
                System.out.println("❌ Payment failed sans paiementId en metadata (aucune MAJ BDD).");
            }
        }

        return ResponseEntity.ok("Webhook reçu");
    }
}
