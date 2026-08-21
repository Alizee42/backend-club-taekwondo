package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.service.jpa.PaiementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PaiementService paiementService;
    private final Environment environment;
    private final ClubRepository clubRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripeWebhookController(PaiementService paiementService, Environment environment, ClubRepository clubRepository) {
        this.paiementService = paiementService;
        this.environment = environment;
        this.clubRepository = clubRepository;
    }

    @PostConstruct
    public void validateWebhookSecret() {
        boolean missing = endpointSecret == null || endpointSecret.isBlank()
                || endpointSecret.toLowerCase().contains("dummy")
                || endpointSecret.toLowerCase().contains("fake");
        if (!missing) {
            return;
        }

        boolean isProd = List.of(environment.getActiveProfiles()).contains("docker");
        if (isProd) {
            throw new IllegalStateException(
                    "STRIPE_WEBHOOK_SECRET est absent ou factice. " +
                            "Definissez STRIPE_WEBHOOK_SECRET (whsec_...) dans vos variables d'environnement.");
        }
        log.warn("[STRIPE] secret webhook non configure en local. Les webhooks seront rejetes.");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        if (endpointSecret == null || endpointSecret.isBlank()) {
            log.warn("[STRIPE] secret webhook absent.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret manquant");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.warn("[STRIPE] signature webhook invalide: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature invalide");
        }

        final String eventId = event.getId();
        log.info("[STRIPE] webhook mode={} type={} eventId={}",
                event.getLivemode() ? "LIVE" : "TEST", event.getType(), eventId);

        if ("account.updated".equals(event.getType())) {
            return handleAccountUpdated(event);
        }

        var dataObj = event.getDataObjectDeserializer().getObject();
        PaymentIntent pi = null;
        if (dataObj.isPresent() && dataObj.get() instanceof PaymentIntent paymentIntent) {
            pi = paymentIntent;
        } else {
            try {
                JsonNode root = objectMapper.readTree(payload);
                JsonNode obj = root.path("data").path("object");
                String type = event.getType() == null ? "" : event.getType();

                String piId = null;
                if (type.startsWith("payment_intent.")) {
                    piId = obj.path("id").asText(null);
                } else if (type.startsWith("charge.")) {
                    piId = obj.path("payment_intent").asText(null);
                }

                if (piId != null && !piId.isBlank()) {
                    log.debug("[STRIPE] fallback retrieve PaymentIntent id={} type={}", piId, type);
                    pi = PaymentIntent.retrieve(piId);
                } else {
                    log.error("[STRIPE] impossible d'extraire payment_intent id du payload.");
                }
            } catch (Exception ex) {
                log.error("[STRIPE] echec fallback JSON/retrieve: {}", ex.getMessage());
            }
        }

        if (pi == null) {
            log.error("[STRIPE] impossible de deserialiser PaymentIntent.");
            return ResponseEntity.ok("ignored");
        }

        final String piId = pi.getId();
        final Map<String, String> md = pi.getMetadata();
        final String paiementIdStr = md != null ? md.get("paiementId") : null;
        final String echeanceIdStr = md != null ? md.get("echeanceId") : null;

        log.debug("[STRIPE] paymentIntent={} metadata[paiementId={}, echeanceId={}]",
                piId, paiementIdStr, echeanceIdStr);

        if ("payment_intent.succeeded".equals(event.getType())) {
            if (isBlank(paiementIdStr)) {
                log.warn("[STRIPE] webhook succeeded sans paiementId.");
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
                log.warn("[STRIPE] paiement introuvable id={}", paiementId);
                return ResponseEntity.ok("paiement-not-found");
            }

            final Paiement paiement = opt.get();
            dumpPaiementState("BEFORE", paiement);

            if ("paye".equalsIgnoreCase(normalize(safe(paiement.getStatut())))
                    && (paiement.getMontantRestant() == null || paiement.getMontantRestant() <= 0.0)) {
                log.info("[STRIPE] paiement deja solde, ignore id={}", paiementId);
                return ResponseEntity.ok("already-paid");
            }

            final Long receivedCents = pi.getAmountReceived();
            final String currency = safe(pi.getCurrency());
            if (!"eur".equalsIgnoreCase(currency) || receivedCents == null) {
                log.warn("[STRIPE] devise ou montant invalides currency={} received={}", currency, receivedCents);
                return ResponseEntity.ok("amount-currency-mismatch");
            }

            if ("ECHELONNE".equalsIgnoreCase(safe(paiement.getType()))
                    && paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {

                Echeance cible = resolveEcheance(paiement, piId, echeanceIdStr);
                if (cible == null) {
                    log.warn("[STRIPE] aucune echeance cible trouvee.");
                    return ResponseEntity.ok("no-ech-found");
                }

                final long expectedCents = toCentsHalfUp(safeDouble(cible.getMontant()));
                if (!Objects.equals(receivedCents, expectedCents)) {
                    log.warn("[STRIPE] amount mismatch echeance={} expected={} received={}",
                            cible.getNumero(), expectedCents, receivedCents);
                    return ResponseEntity.ok("amount-mismatch");
                }

                if ("paye".equalsIgnoreCase(normalize(safe(cible.getStatut())))) {
                    log.info("[STRIPE] echeance deja payee numero={}", cible.getNumero());
                    return ResponseEntity.ok("installment-already-paid");
                }

                cible.setStatut("pay\u00e9");
                cible.setDatePaiementReel(LocalDate.now());
                cible.setModePaiement("CB");
                if (isBlank(cible.getReference())) {
                    cible.setReference(piId);
                }

                long nbRestantes = paiement.getEcheances().stream()
                        .filter(e -> !"paye".equalsIgnoreCase(normalize(safe(e.getStatut()))))
                        .count();
                double restant = paiement.getEcheances().stream()
                        .filter(e -> !"paye".equalsIgnoreCase(normalize(safe(e.getStatut()))))
                        .mapToDouble(Echeance::getMontant)
                        .sum();

                paiement.setMontantRestant(restant);
                paiement.setEcheancesRestantes((int) nbRestantes);
                paiement.setModePaiement("CB");
                paiement.setStatut(nbRestantes == 0 ? "pay\u00e9" : "en attente");

                try {
                    if (isBlank(paiement.getPaymentIntentId())) {
                        paiement.setPaymentIntentId(piId);
                    }
                    String latestChargeId = pi.getLatestCharge();
                    if (!isBlank(latestChargeId)) {
                        paiement.setChargeId(latestChargeId);
                        try {
                            Charge c = Charge.retrieve(latestChargeId);
                            if (c != null && !isBlank(c.getReceiptUrl())) {
                                paiement.setReceiptUrl(c.getReceiptUrl());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    if (!isBlank(pi.getStatus())) {
                        paiement.setStripeStatus(pi.getStatus());
                    }
                } catch (Exception ignored) {
                }

                paiementService.save(paiement);
                dumpPaiementState("AFTER", paiement);
                log.info("[STRIPE] echeance soldee numero={} paiement={} restantes={} restant={}",
                        cible.getNumero(), paiement.getId(), nbRestantes, restant);
                return ResponseEntity.ok("ok");
            }

            final long expectedCents = toCentsHalfUp(safeDouble(paiement.getMontantTotal()));
            if (!Objects.equals(receivedCents, expectedCents)) {
                log.warn("[STRIPE] amount mismatch unique expected={} received={}", expectedCents, receivedCents);
                return ResponseEntity.ok("amount-mismatch");
            }

            paiement.setStatut("pay\u00e9");
            paiement.setMontantRestant(0.0);
            paiement.setDatePaiement(LocalDate.now());
            paiement.setModePaiement("CB");

            try {
                if (isBlank(paiement.getPaymentIntentId())) {
                    paiement.setPaymentIntentId(piId);
                }
                String latestChargeId = pi.getLatestCharge();
                if (!isBlank(latestChargeId)) {
                    paiement.setChargeId(latestChargeId);
                    try {
                        Charge c = Charge.retrieve(latestChargeId);
                        if (c != null && !isBlank(c.getReceiptUrl())) {
                            paiement.setReceiptUrl(c.getReceiptUrl());
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (!isBlank(pi.getStatus())) {
                    paiement.setStripeStatus(pi.getStatus());
                }
            } catch (Exception ignored) {
            }

            paiementService.save(paiement);
            dumpPaiementState("AFTER", paiement);
            log.info("[STRIPE] paiement unique solde id={}", paiement.getId());
        }

        if ("payment_intent.payment_failed".equals(event.getType())) {
            String reason = pi.getLastPaymentError() != null
                    ? pi.getLastPaymentError().getMessage()
                    : "unknown";
            log.warn("[STRIPE] payment failed paymentIntent={} reason={} metadata={}", piId, reason, md);
        }

        return ResponseEntity.ok("Webhook recu");
    }

    // Suit l'etat de verification des comptes Stripe Connect des clubs : un compte
    // "Standard" peut exister (cree a l'onboarding) sans etre encore autorise a
    // encaisser (charges_enabled=false) tant que Stripe n'a pas fini sa verification.
    private ResponseEntity<String> handleAccountUpdated(Event event) {
        var dataObj = event.getDataObjectDeserializer().getObject();
        if (dataObj.isEmpty() || !(dataObj.get() instanceof Account account)) {
            log.warn("[STRIPE-CONNECT] account.updated recu sans objet Account deserialisable, ignore.");
            return ResponseEntity.ok("ignored");
        }

        Club club = clubRepository.findByStripeAccountId(account.getId());
        if (club == null) {
            log.warn("[STRIPE-CONNECT] account.updated pour un compte inconnu localement : {}", account.getId());
            return ResponseEntity.ok("unknown-account");
        }

        boolean chargesEnabled = Boolean.TRUE.equals(account.getChargesEnabled());
        club.setStripeChargesEnabled(chargesEnabled);
        clubRepository.save(club);
        log.info("[STRIPE-CONNECT] club {} (compte {}) chargesEnabled={}", club.getName(), account.getId(), chargesEnabled);

        return ResponseEntity.ok("Webhook recu");
    }

    private Echeance resolveEcheance(Paiement paiement, String piId, String echeanceIdStr) {
        for (Echeance e : paiement.getEcheances()) {
            if (piId.equals(safe(e.getReference()))) {
                return e;
            }
        }

        if (!isBlank(echeanceIdStr)) {
            try {
                long echId = Long.parseLong(echeanceIdStr);
                return paiement.getEcheances().stream()
                        .filter(e -> Objects.equals(e.getId(), echId))
                        .findFirst()
                        .orElse(null);
            } catch (NumberFormatException ignored) {
            }
        }

        return paiement.getEcheances().stream()
                .filter(e -> !"paye".equalsIgnoreCase(normalize(safe(e.getStatut()))))
                .sorted(Comparator.comparingInt(Echeance::getNumero))
                .findFirst()
                .orElse(null);
    }

    private void dumpPaiementState(String label, Paiement p) {
        log.debug("[STRIPE] {} paiement id={} type={} statut={} restant={} echRestantes={}",
                label,
                p.getId(),
                safe(p.getType()),
                safe(p.getStatut()),
                p.getMontantRestant() == null ? 0.0 : p.getMontantRestant(),
                p.getEcheancesRestantes() == null ? -1 : p.getEcheancesRestantes());

        if (p.getEcheances() != null) {
            p.getEcheances().stream()
                    .sorted(Comparator.comparingInt(Echeance::getNumero))
                    .forEach(e -> log.debug("[STRIPE] echeance numero={} id={} statut={} montant={} ref={}",
                            e.getNumero(), e.getId(), safe(e.getStatut()),
                            safeDouble(e.getMontant()), safe(e.getReference())));
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u00e9', 'e')
                .replace('\u00c9', 'E');
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double safeDouble(Double d) {
        return d == null ? 0.0 : d;
    }

    private static long toCentsHalfUp(double euros) {
        return BigDecimal.valueOf(euros).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
