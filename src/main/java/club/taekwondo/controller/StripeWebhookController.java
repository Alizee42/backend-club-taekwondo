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

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        if (endpointSecret == null || endpointSecret.isBlank()) {
            System.err.println("⚠️ stripe.webhook.secret non configuré !");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret manquant");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("❌ Signature Stripe invalide : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        }

        System.out.println("📦 Stripe Mode : " + (event.getLivemode() ? "LIVE" : "TEST"));
        System.out.println("ℹ️ Événement Stripe : " + event.getType());

        var dataObj = event.getDataObjectDeserializer().getObject();
        if (dataObj.isEmpty() || !(dataObj.get() instanceof PaymentIntent pi)) {
            System.err.println("❌ Impossible de désérialiser PaymentIntent.");
            return ResponseEntity.ok("ignored");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            Map<String, String> md = pi.getMetadata();
            String paiementIdStr = md != null ? md.get("paiementId") : null;
            if (paiementIdStr == null || paiementIdStr.isBlank()) {
                System.out.println("⚠️ succeeded: paiementId manquant dans metadata.");
                return ResponseEntity.ok("no-paiementId");
            }

            Long paiementId;
            try { paiementId = Long.parseLong(paiementIdStr); }
            catch (NumberFormatException nfe) { return ResponseEntity.ok("invalid-paiementId"); }

            Optional<Paiement> opt = paiementService.getById(paiementId);
            if (opt.isEmpty()) {
                System.out.println("❌ Paiement introuvable id=" + paiementId);
                return ResponseEntity.ok("paiement-not-found");
            }
            Paiement paiement = opt.get();

            // Idempotence simple : si déjà soldé & plus rien à payer → on ignore
            if ("payé".equalsIgnoreCase(paiement.getStatut())
                    && (paiement.getMontantRestant() == null || paiement.getMontantRestant() <= 0.0)) {
                System.out.println("↩️ Déjà payé, on ignore.");
                return ResponseEntity.ok("already-paid");
            }

            // ---- Vérification devise & montant attendus ----
            String currency = pi.getCurrency(); // "eur"
            Long received = pi.getAmountReceived() != null ? pi.getAmountReceived() : pi.getAmount(); // centimes
            if (currency == null || !currency.equalsIgnoreCase("eur") || received == null) {
                System.err.println("❌ Devise/amount invalides: currency=" + currency + ", received=" + received);
                return ResponseEntity.ok("amount-currency-mismatch");
            }

            long expected;
            if ("ECHELONNE".equalsIgnoreCase(paiement.getType()) && paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
                var firstUnpaid = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .sorted(Comparator.comparingInt(Echeance::getNumero))
                        .findFirst();
                if (firstUnpaid.isEmpty()) {
                    return ResponseEntity.ok("no-unpaid-installment");
                }
                expected = Math.round(firstUnpaid.get().getMontant() * 100.0);
            } else {
                if (paiement.getMontantTotal() == null || paiement.getMontantTotal() <= 0) {
                    return ResponseEntity.ok("invalid-total");
                }
                expected = Math.round(paiement.getMontantTotal() * 100.0);
            }

            if (received.longValue() != expected) {
                System.err.println("❌ Amount mismatch: expected=" + expected + ", received=" + received);
                return ResponseEntity.ok("amount-mismatch");
            }

            // ---- Mise à jour BDD ----
            paiement.setModePaiement("CB"); // confirmé: c'est un paiement par carte Stripe

            if ("ECHELONNE".equalsIgnoreCase(paiement.getType()) && paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
                // solder la 1ʳᵉ échéance non payée
                paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .sorted(Comparator.comparingInt(Echeance::getNumero))
                        .findFirst()
                        .ifPresent(e -> {
                            e.setStatut("payé");
                            e.setDatePaiementReel(java.time.LocalDate.now());
                            e.setModePaiement("CB");
                        });

                // Recalcul montant restant + nb échéances restantes
                long nbRestantes = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut())).count();
                double restant = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .mapToDouble(Echeance::getMontant).sum();

                paiement.setMontantRestant(restant);
                paiement.setEcheancesRestantes((int) nbRestantes);
                paiement.setStatut(nbRestantes == 0 ? "payé" : "en attente");
            } else {
                // paiement unique
                paiement.setStatut("payé");
                paiement.setMontantRestant(0.0);
                paiement.setDatePaiement(java.time.LocalDate.now());
            }

            paiementService.save(paiement);
            System.out.printf("✅ Paiement confirmé: ID=%d, Mode=%s, Statut=%s%n",
                    paiement.getId(), paiement.getModePaiement(), paiement.getStatut());
        }

        if ("payment_intent.payment_failed".equals(event.getType())) {
            Map<String, String> md = pi.getMetadata();
            String paiementIdStr = md != null ? md.get("paiementId") : null;
            String reason = (pi.getLastPaymentError() != null) ? pi.getLastPaymentError().getMessage() : "unknown";

            if (paiementIdStr != null && !paiementIdStr.isBlank()) {
                try {
                    Long paiementId = Long.parseLong(paiementIdStr);
                    Optional<Paiement> optional = paiementService.getById(paiementId);
                    if (optional.isPresent()) {
                        System.out.println("❌ Paiement échoué (paiementId=" + paiementId + ") : " + reason);
                        // On laisse "en attente" pour permettre un nouvel essai
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
