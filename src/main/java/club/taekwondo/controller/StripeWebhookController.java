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
            System.err.println("⚠️ stripe.webhook.secret non configuré !");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret manquant");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } 
        catch (SignatureVerificationException e) {
            System.err.println("❌ Signature Stripe invalide : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        }

        final String eventId = event.getId();
        System.out.println("📦 Stripe Mode : " + (event.getLivemode() ? "LIVE" : "TEST"));
        System.out.println("ℹ️ Événement Stripe : " + event.getType() + " | eventId=" + eventId);

        var dataObj = event.getDataObjectDeserializer().getObject();
        if (dataObj.isEmpty() || !(dataObj.get() instanceof PaymentIntent pi)) {
            System.err.println("❌ Impossible de désérialiser PaymentIntent.");
            return ResponseEntity.ok("ignored");
        }

        final String piId = pi.getId();
        final Map<String, String> md = pi.getMetadata();
        final String paiementIdStr = md != null ? md.get("paiementId") : null;
        final String echeanceIdStr = md != null ? md.get("echeanceId") : null;

        System.out.printf("🔎 PI=%s | metadata={paiementId=%s, echeanceId=%s}%n", piId, paiementIdStr, echeanceIdStr);

        if ("payment_intent.succeeded".equals(event.getType())) {

            if (isBlank(paiementIdStr)) {
                System.out.println("⚠️ succeeded: paiementId manquant dans metadata.");
                return ResponseEntity.ok("no-paiementId");
            }

            final Long paiementId;
            try { paiementId = Long.parseLong(paiementIdStr); }
            catch (NumberFormatException nfe) { return ResponseEntity.ok("invalid-paiementId"); }

            final Optional<Paiement> opt = paiementService.getById(paiementId);
            if (opt.isEmpty()) {
                System.out.println("❌ Paiement introuvable id=" + paiementId);
                return ResponseEntity.ok("paiement-not-found");
            }
            final Paiement paiement = opt.get();

            dumpPaiementState("BEFORE", paiement);

            // Devise / montant reçu
            final String currency = safe(pi.getCurrency());
            final Long receivedCents = pi.getAmountReceived() != null ? pi.getAmountReceived() : pi.getAmount();
            if (!"eur".equalsIgnoreCase(currency) || receivedCents == null) {
                System.err.println("❌ Devise/amount invalides: currency=" + currency + ", received=" + receivedCents);
                return ResponseEntity.ok("amount-currency-mismatch");
            }

            Echeance bound = null;
            if (paiement.getEcheances() != null) {
                for (Echeance e : paiement.getEcheances()) {
                    if (piId.equals(safe(e.getReference()))) {
                        bound = e;
                        break;
                    }
                }
            }
            if (bound != null) {
                System.out.printf("🧷 PI déjà lié à l’échéance #%d (id=%d, statut=%s).%n",
                        bound.getNumero(), bound.getId(), safe(bound.getStatut()));
                if ("payé".equalsIgnoreCase(safe(bound.getStatut()))) {
                    System.out.println("↩️ already-processed (même PI, même échéance).");
                    return ResponseEntity.ok("already-processed");
                }
                System.out.println("↩️ reprise de la même échéance liée pour finaliser.");
            }

            if ("ECHELONNE".equalsIgnoreCase(safe(paiement.getType()))
                    && paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {

                Echeance cible = bound; // peut être déjà fixé par le garde-fou

                if (cible == null && !isBlank(echeanceIdStr)) {
                    try {
                        long echId = Long.parseLong(echeanceIdStr);
                        for (Echeance e : paiement.getEcheances()) {
                            if (e.getId() != null && e.getId().longValue() == echId) {
                                cible = e; break;
                            }
                        }
                        if (cible == null) {
                            System.out.println("❌ metadata.echeanceId inconnu pour ce paiement.");
                            return ResponseEntity.ok("unknown-echeanceId");
                        }
                        System.out.printf("🎯 Cible par metadata: ech#%d (id=%d)%n", cible.getNumero(), cible.getId());
                    } catch (NumberFormatException nfe) {
                        System.out.println("⚠️ echeanceId invalide dans metadata: " + echeanceIdStr);
                        return ResponseEntity.ok("invalid-echeanceId");
                    }
                }

                if (cible == null) {
                    cible = paiement.getEcheances().stream()
                            .filter(e -> !"payé".equalsIgnoreCase(safe(e.getStatut())))
                            .sorted(Comparator.comparingInt(Echeance::getNumero))
                            .findFirst()
                            .orElse(null);
                    if (cible == null) {
                        System.out.println("↩️ Aucune échéance impayée.");
                        return ResponseEntity.ok("no-unpaid-installment");
                    }
                    System.out.printf("🎯 Cible par fallback: ech#%d (id=%d)%n", cible.getNumero(), cible.getId());
                }

                if (!isBlank(cible.getReference()) && !Objects.equals(cible.getReference(), piId)) {
                    System.out.printf("↩️ PI %s ignoré (échéance %d déjà liée à %s)%n",
                            piId, cible.getNumero(), cible.getReference());
                    return ResponseEntity.ok("installment-already-bound-to-other-pi");
                }

                final long expectedCents = toCentsHALF_UP(safeDouble(cible.getMontant()));
                if (!Objects.equals(receivedCents, expectedCents)) {
                    System.err.printf("❌ Amount mismatch ech#%d: expected=%d, received=%d%n",
                            cible.getNumero(), expectedCents, receivedCents);
                    return ResponseEntity.ok("amount-mismatch");
                }

                if ("payé".equalsIgnoreCase(safe(cible.getStatut()))) {
                    System.out.printf("↩️ Échéance #%d déjà payée, on ignore.%n", cible.getNumero());
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
                System.out.printf("✅ Echéance #%d soldée pour paiement %d. Restantes=%d, Restant=%.2f%n",
                        cible.getNumero(), paiement.getId(), nbRestantes, restant);
                return ResponseEntity.ok("ok");
            }

            // ===== Paiement UNIQUE =====
            final long expectedCents = toCentsHALF_UP(safeDouble(paiement.getMontantTotal()));
            if (!Objects.equals(receivedCents, expectedCents)) {
                System.err.printf("❌ Amount mismatch UNIQUE: expected=%d, received=%d%n", expectedCents, receivedCents);
                return ResponseEntity.ok("amount-mismatch");
            }
            if ("payé".equalsIgnoreCase(safe(paiement.getStatut()))
                    && (paiement.getMontantRestant() == null || paiement.getMontantRestant() <= 0.0)) {
                System.out.println("↩️ Paiement unique déjà soldé.");
                return ResponseEntity.ok("already-paid");
            }

            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setDatePaiement(LocalDate.now());
            paiement.setModePaiement("CB");

            paiementService.save(paiement);
            dumpPaiementState("AFTER", paiement);
            System.out.printf("✅ Paiement UNIQUE %d soldé%n", paiement.getId());
        }

        if ("payment_intent.payment_failed".equals(event.getType())) {
            String reason = (pi.getLastPaymentError() != null) ? pi.getLastPaymentError().getMessage() : "unknown";
            System.out.printf("❌ Payment failed PI=%s | reason=%s | metadata=%s%n", piId, reason, md);
        }

        return ResponseEntity.ok("Webhook reçu");
    }

    // -------- logs utiles --------
    private static void dumpPaiementState(String label, Paiement p) {
        System.out.printf("🧾 %s Paiement id=%d type=%s statut=%s restant=%.2f echRestantes=%d%n",
                label, p.getId(), safe(p.getType()), safe(p.getStatut()),
                p.getMontantRestant() == null ? 0.0 : p.getMontantRestant(),
                p.getEcheancesRestantes() == null ? -1 : p.getEcheancesRestantes());
        if (p.getEcheances() != null) {
            p.getEcheances().stream()
                    .sorted(Comparator.comparingInt(Echeance::getNumero))
                    .forEach(e -> System.out.printf("   • ech#%d id=%d statut=%s montant=%.2f ref=%s%n",
                            e.getNumero(), e.getId(), safe(e.getStatut()),
                            safeDouble(e.getMontant()), safe(e.getReference())));
        }
    }

    // -------- utils --------
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static double safeDouble(Double d) { return d == null ? 0.0 : d; }
    private static long toCentsHALF_UP(double euros) {
        return BigDecimal.valueOf(euros).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}

