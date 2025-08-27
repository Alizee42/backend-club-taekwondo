package club.taekwondo.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @Value("${stripe.default.currency:eur}")
    private String defaultCurrency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String getPublicKey() {
        return stripePublicKey;
    }

    /** Crée un PaymentIntent avec metadata + idempotency key fournie */
    public PaymentIntent createPaymentIntentWithMetadata(Map<String, Object> req, String idempotencyKey) throws StripeException {
        long amount = parseLongStrict(req.get("amount"), "amount (centimes)");
        if (amount <= 0) throw new IllegalArgumentException("Montant invalide (centimes).");

        String currency = Objects.toString(req.getOrDefault("currency", defaultCurrency), defaultCurrency).toLowerCase();
        if (!List.of("eur", "usd").contains(currency)) {
            throw new IllegalArgumentException("Devise non supportée : " + currency);
        }

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                );

        // Copier metadata si fournie
        Object mdObj = req.get("metadata");
        if (mdObj instanceof Map<?, ?> md) {
            for (Map.Entry<?, ?> e : md.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    builder.putMetadata(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }

        RequestOptions opts = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey) // clé passée par le controller (inclut échéance)
                .build();

        return PaymentIntent.create(builder.build(), opts);
    }

    /** Utilitaire : récupère le client_secret (non utilisé si on check le statut côté controller) */
    public String retrieveClientSecret(String paymentIntentId) {
        try {
            if (paymentIntentId == null || paymentIntentId.isBlank()) return null;
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return intent != null ? intent.getClientSecret() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Helpers
    private static long parseLongStrict(Object o, String fieldName) {
        try {
            if (o instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            throw new IllegalArgumentException("Champ invalide '" + fieldName + "': " + o);
        }
    }
}
