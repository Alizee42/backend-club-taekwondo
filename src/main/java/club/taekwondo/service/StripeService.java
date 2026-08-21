package club.taekwondo.service;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @Value("${stripe.default.currency:eur}")
    private String defaultCurrency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        if (stripeApiKey == null || stripeApiKey.isBlank() || stripeApiKey.toLowerCase().contains("dummy")) {
            log.warn("[STRIPE] cle API absente ou factice. Definissez STRIPE_API_KEY pour les tests locaux.");
        } else {
            log.info("[STRIPE] cle API configuree.");
        }
    }

    public String getPublicKey() {
        log.debug("[STRIPE] lecture de la cle publique.");
        return stripePublicKey;
    }

    public PaymentIntent createPaymentIntentWithMetadata(Map<String, Object> req, String idempotencyKey) throws StripeException {
        return createPaymentIntentWithMetadata(req, idempotencyKey, null);
    }

    /**
     * @param stripeAccountId si non-null, la charge est creee directement sur le compte
     *                        Stripe connecte du club (direct charge) plutot que sur le
     *                        compte plateforme partage.
     */
    public PaymentIntent createPaymentIntentWithMetadata(Map<String, Object> req, String idempotencyKey, String stripeAccountId) throws StripeException {
        log.info("[STRIPE] creation PaymentIntent idempotencyKey={} stripeAccountId={}", idempotencyKey, stripeAccountId);

        long amount = parseLongStrict(req.get("amount"), "amount (centimes)");
        if (amount <= 0) {
            throw new IllegalArgumentException("Montant invalide (centimes).");
        }

        String currency = Objects.toString(req.getOrDefault("currency", defaultCurrency), defaultCurrency).toLowerCase();
        if (!List.of("eur", "usd").contains(currency)) {
            throw new IllegalArgumentException("Devise non supportee : " + currency);
        }

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                );

        Object mdObj = req.get("metadata");
        if (mdObj instanceof Map<?, ?> md) {
            for (Map.Entry<?, ?> e : md.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    builder.putMetadata(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }

        log.debug("[STRIPE] requete PaymentIntent amount={} currency={}", amount, currency);

        RequestOptions.RequestOptionsBuilder optsBuilder = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey);
        if (stripeAccountId != null && !stripeAccountId.isBlank()) {
            optsBuilder.setStripeAccount(stripeAccountId);
        }

        PaymentIntent paymentIntent = PaymentIntent.create(builder.build(), optsBuilder.build());
        log.info("[STRIPE] PaymentIntent cree id={}", paymentIntent.getId());
        return paymentIntent;
    }

    public String retrieveClientSecret(String paymentIntentId) {
        return retrieveClientSecret(paymentIntentId, null);
    }

    public String retrieveClientSecret(String paymentIntentId, String stripeAccountId) {
        log.debug("[STRIPE] recuperation clientSecret pour PaymentIntent={} stripeAccountId={}", paymentIntentId, stripeAccountId);
        try {
            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                return null;
            }
            PaymentIntent intent = (stripeAccountId != null && !stripeAccountId.isBlank())
                    ? PaymentIntent.retrieve(paymentIntentId, RequestOptions.builder().setStripeAccount(stripeAccountId).build())
                    : PaymentIntent.retrieve(paymentIntentId);
            return intent != null ? intent.getClientSecret() : null;
        } catch (Exception e) {
            log.warn("[STRIPE] erreur recuperation clientSecret: {}", e.getMessage());
            return null;
        }
    }

    public boolean isConfigured() {
        return stripeApiKey != null && !stripeApiKey.isBlank() && !stripeApiKey.toLowerCase().contains("dummy");
    }

    public Optional<String> getReceiptUrl(Paiement p) {
        if (p.getReceiptUrl() != null && !p.getReceiptUrl().isBlank()) {
            return Optional.of(p.getReceiptUrl());
        }

        String piId = null;
        if (p.getEcheances() != null) {
            for (int i = p.getEcheances().size() - 1; i >= 0; i--) {
                Echeance e = p.getEcheances().get(i);
                if ("paye".equalsIgnoreCase(normalize(e.getStatut()))
                        && e.getReference() != null && !e.getReference().isBlank()) {
                    piId = e.getReference();
                    break;
                }
            }
        }

        if (piId == null) {
            piId = p.getPaymentIntentId();
        }
        if (piId == null || piId.isBlank()) {
            return Optional.empty();
        }

        String stripeAccountId = (p.getClub() != null) ? p.getClub().getStripeAccountId() : null;
        RequestOptions opts = (stripeAccountId != null && !stripeAccountId.isBlank())
                ? RequestOptions.builder().setStripeAccount(stripeAccountId).build()
                : null;

        try {
            PaymentIntent pi = opts != null ? PaymentIntent.retrieve(piId, opts) : PaymentIntent.retrieve(piId);
            String latestChargeId = pi.getLatestCharge();
            if (latestChargeId != null && !latestChargeId.isBlank()) {
                Charge charge = opts != null ? Charge.retrieve(latestChargeId, opts) : Charge.retrieve(latestChargeId);
                if (charge != null && charge.getReceiptUrl() != null && !charge.getReceiptUrl().isBlank()) {
                    return Optional.of(charge.getReceiptUrl());
                }
            }

            Object latestObj = pi.getLatestChargeObject();
            if (latestObj instanceof Charge charge) {
                String url = charge.getReceiptUrl();
                if (url != null && !url.isBlank()) {
                    return Optional.of(url);
                }
            }
        } catch (Exception e) {
            log.warn("[STRIPE] erreur recuperation receiptUrl pour paiement {}: {}", p.getId(), e.getMessage());
        }

        return Optional.empty();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u00e9', 'e')
                .replace('\u00c9', 'E');
    }

    private long parseLongStrict(Object o, String fieldName) {
        try {
            if (o instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            log.warn("[STRIPE] champ invalide {}={}", fieldName, o);
            throw new IllegalArgumentException("Champ invalide '" + fieldName + "': " + o);
        }
    }
}
