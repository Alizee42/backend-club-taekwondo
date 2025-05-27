package club.taekwondo.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentIntent createPaymentIntent(Double amount, String currency) throws StripeException {
        System.out.println("Création d'un PaymentIntent avec le montant : " + amount + " et la devise : " + currency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100)) // Montant en centimes
                .setCurrency(currency)
                .addPaymentMethodType("card") // Méthode de paiement acceptée
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        System.out.println("PaymentIntent créé avec succès : " + paymentIntent.getId());
        return paymentIntent;
    }

    public String getPublicKey() {
        return stripePublicKey;
    }
}