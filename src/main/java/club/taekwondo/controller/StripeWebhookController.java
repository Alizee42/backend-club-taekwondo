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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

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
            System.err.println("[STRIPE] ⚠️ stripe.webhook.secret non configuré !");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret manquant");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("[STRIPE] ❌ Signature Stripe invalide : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        }

        final String eventId = event.getId();
        System.out.printf("[STRIPE] 📦 Mode=%s | Événement=%s | eventId=%s%n",
                event.getLivemode() ? "LIVE" : "TEST", event.getType(), eventId);

        var dataObj = event.getDataObjectDeserializer().getObject();
        if (dataObj.isEmpty() || !(dataObj.get() instanceof PaymentIntent pi)) {
            System.err.println("[STRIPE] ❌ Impossible de désérialiser PaymentIntent.");
            return ResponseEntity.ok("ignored");
        }

        final String piId = pi.getId();
        final Map<String, String> md = pi.getMetadata();
        final String paiementIdStr = md != null ? md.get("paiementId") : null;
        final String echeanceIdStr = md != null ? md.get("echeanceId") : null;

        System.out.printf("[STRIPE] 🔎 PI=%s | metadata={paiementId=%s, echeanceId=%s}%n",
                piId, paiementIdStr, echeanceIdStr);

        if ("payment_intent.succeeded".equals(event.getType())) {

            if (isBlank(paiementIdStr)) {
                System.out.println("[STRIPE] ⚠️ succeeded: paiementId manquant dans metadata.");
                return ResponseEntity.ok("no-paiementId");
            }

            final Long paiementId;
            try {
                paiementId = Long.parseLong(paiementIdStr);
            } catch (NumberFormatException nfe) {
                return ResponseEntity.ok("invalid-paiementId");
            }

            final Optional<Paiement> opt = paiementService.getById(paiementId);
            if (opt.isEmpty()) {
                System.out.println("[STRIPE] ❌ Paiement introuvable id=" + paiementId);
                return ResponseEntity.ok("paiement-not-found");
            }
            final Paiement paiement = opt.get();
            dumpPaiementState("BEFORE", paiement);

            // 🔒 Garde-fou : si déjà soldé, on ignore
            if ("payé".equalsIgnoreCase(safe(paiement.getStatut()))
                    && (paiement.getMontantRestant() == null || paiement.getMontantRestant() <= 0.0)) {
                System.out.printf("[STRIPE] ↩️ Paiement id=%d déjà soldé, on ignore.%n", paiementId);
                return ResponseEntity.ok("already-paid");
            }

            // Montant reçu
            final Long receivedCents = pi.getAmountReceived();
            final String currency = safe(pi.getCurrency());
            if (!"eur".equalsIgnoreCase(currency) || receivedCents == null) {
                System.err.printf("[STRIPE] ❌ Devise/amount invalides: currency=%s, received=%s%n",
                        currency, receivedCents);
                return ResponseEntity.ok("amount-currency-mismatch");
            }

            // -------- Paiement échelonné --------
            if ("ECHELONNE".equalsIgnoreCase(safe(paiement.getType()))
                    && paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {

                Echeance cible = resolveEcheance(paiement, piId, echeanceIdStr);
                if (cible == null) {
                    System.out.println("[STRIPE] ❌ Aucune échéance cible trouvée.");
                    return ResponseEntity.ok("no-ech-found");
                }

                final long expectedCents = toCentsHALF_UP(safeDouble(cible.getMontant()));
                if (!Objects.equals(receivedCents, expectedCents)) {
                    System.err.printf("[STRIPE] ❌ Amount mismatch ech#%d: expected=%d, received=%d%n",
                            cible.getNumero(), expectedCents, receivedCents);
                    return ResponseEntity.ok("amount-mismatch");
                }

                if ("payé".equalsIgnoreCase(safe(cible.getStatut()))) {
                    System.out.printf("[STRIPE] ↩️ Échéance #%d déjà payée.%n", cible.getNumero());
                    return ResponseEntity.ok("installment-already-paid");
                }

                cible.setStatut("payé");
                cible.setDatePaiementReel(LocalDate.now());
                cible.setModePaiement("CB");
                if (isBlank(cible.getReference())) {
                    cible.setReference(piId);
                }

                long nbRestantes = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(safe(e.getStatut()))).count();
                double restant = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(safe(e.getStatut())))
                        .mapToDouble(Echeance::getMontant).sum();

                paiement.setMontantRestant(restant);
                paiement.setEcheancesRestantes((int) nbRestantes);
                paiement.setModePaiement("CB");
                paiement.setStatut(nbRestantes == 0 ? "payé" : "en attente");

                paiementService.save(paiement);
                dumpPaiementState("AFTER", paiement);
                System.out.printf("[STRIPE] ✅ Échéance #%d soldée pour paiement %d. Restantes=%d, Restant=%.2f%n",
                        cible.getNumero(), paiement.getId(), nbRestantes, restant);
                return ResponseEntity.ok("ok");
            }

            // -------- Paiement unique --------
            final long expectedCents = toCentsHALF_UP(safeDouble(paiement.getMontantTotal()));
            if (!Objects.equals(receivedCents, expectedCents)) {
                System.err.printf("[STRIPE] ❌ Amount mismatch UNIQUE: expected=%d, received=%d%n",
                        expectedCents, receivedCents);
                return ResponseEntity.ok("amount-mismatch");
            }

            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setDatePaiement(LocalDate.now());
            paiement.setModePaiement("CB");

            paiementService.save(paiement);
            dumpPaiementState("AFTER", paiement);
            System.out.printf("[STRIPE] ✅ Paiement UNIQUE %d soldé%n", paiement.getId());
        }

        if ("payment_intent.payment_failed".equals(event.getType())) {
            String reason = (pi.getLastPaymentError() != null)
                    ? pi.getLastPaymentError().getMessage()
                    : "unknown";
            System.out.printf("[STRIPE] ❌ Payment failed PI=%s | reason=%s | metadata=%s%n",
                    piId, reason, md);
        }

        return ResponseEntity.ok("Webhook reçu");
    }

    // -------- Helpers --------
    private Echeance resolveEcheance(Paiement paiement, String piId, String echeanceIdStr) {
        // Échéance déjà liée à ce PI ?
        for (Echeance e : paiement.getEcheances()) {
            if (piId.equals(safe(e.getReference()))) {
                return e;
            }
        }
        // Par metadata
        if (!isBlank(echeanceIdStr)) {
            try {
                long echId = Long.parseLong(echeanceIdStr);
                return paiement.getEcheances().stream()
                        .filter(e -> Objects.equals(e.getId(), echId))
                        .findFirst().orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        // Sinon première impayée
        return paiement.getEcheances().stream()
                .filter(e -> !"payé".equalsIgnoreCase(safe(e.getStatut())))
                .sorted(Comparator.comparingInt(Echeance::getNumero))
                .findFirst().orElse(null);
    }

    private static void dumpPaiementState(String label, Paiement p) {
        System.out.printf("[STRIPE] 🧾 %s Paiement id=%d type=%s statut=%s restant=%.2f echRestantes=%d%n",
                label, p.getId(), safe(p.getType()), safe(p.getStatut()),
                p.getMontantRestant() == null ? 0.0 : p.getMontantRestant(),
                p.getEcheancesRestantes() == null ? -1 : p.getEcheancesRestantes());
        if (p.getEcheances() != null) {
            p.getEcheances().stream()
                    .sorted(Comparator.comparingInt(Echeance::getNumero))
                    .forEach(e -> System.out.printf("[STRIPE]    • ech#%d id=%d statut=%s montant=%.2f ref=%s%n",
                            e.getNumero(), e.getId(), safe(e.getStatut()),
                            safeDouble(e.getMontant()), safe(e.getReference())));
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static double safeDouble(Double d) { return d == null ? 0.0 : d; }
    private static long toCentsHALF_UP(double euros) {
        return BigDecimal.valueOf(euros).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
