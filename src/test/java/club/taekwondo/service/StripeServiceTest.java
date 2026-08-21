package club.taekwondo.service;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StripeServiceTest {

    private StripeService stripeService;

    private MockedStatic<PaymentIntent> paymentIntentStatic;
    private MockedStatic<Charge> chargeStatic;

    @BeforeEach
    void setup() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", "sk_test_dummy");
        ReflectionTestUtils.setField(stripeService, "stripePublicKey", "pk_test_dummy");
        ReflectionTestUtils.setField(stripeService, "defaultCurrency", "eur");
    }

    @AfterEach
    void tearDown() {
        if (paymentIntentStatic != null) paymentIntentStatic.close();
        if (chargeStatic != null) chargeStatic.close();
    }

    // ── isConfigured ─────────────────────────────────────────────────────────

    @Test
    void isConfigured_withDummyKey_returnsFalse() {
        assertFalse(stripeService.isConfigured());
    }

    @Test
    void isConfigured_withRealKey_returnsTrue() {
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", "sk_live_realkey123");
        assertTrue(stripeService.isConfigured());
    }

    @Test
    void isConfigured_withBlankKey_returnsFalse() {
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", "   ");
        assertFalse(stripeService.isConfigured());
    }

    @Test
    void isConfigured_withNullKey_returnsFalse() {
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", null);
        assertFalse(stripeService.isConfigured());
    }

    // ── getPublicKey ──────────────────────────────────────────────────────────

    @Test
    void getPublicKey_returnsConfiguredKey() {
        assertEquals("pk_test_dummy", stripeService.getPublicKey());
    }

    // ── createPaymentIntentWithMetadata — validation arguments ────────────────

    @Test
    void createPaymentIntent_withZeroAmount_throwsIllegalArgument() {
        Map<String, Object> req = Map.of("amount", 0L, "currency", "eur");
        assertThrows(IllegalArgumentException.class,
                () -> stripeService.createPaymentIntentWithMetadata(req, "idem-key-1"));
    }

    @Test
    void createPaymentIntent_withNegativeAmount_throwsIllegalArgument() {
        Map<String, Object> req = Map.of("amount", -100L, "currency", "eur");
        assertThrows(IllegalArgumentException.class,
                () -> stripeService.createPaymentIntentWithMetadata(req, "idem-key-2"));
    }

    @Test
    void createPaymentIntent_withInvalidCurrency_throwsIllegalArgument() {
        Map<String, Object> req = Map.of("amount", 1000L, "currency", "gbp");
        assertThrows(IllegalArgumentException.class,
                () -> stripeService.createPaymentIntentWithMetadata(req, "idem-key-3"));
    }

    @Test
    void createPaymentIntent_withInvalidAmountType_throwsIllegalArgument() {
        Map<String, Object> req = Map.of("amount", "not-a-number", "currency", "eur");
        assertThrows(IllegalArgumentException.class,
                () -> stripeService.createPaymentIntentWithMetadata(req, "idem-key-4"));
    }

    // ── getReceiptUrl ─────────────────────────────────────────────────────────

    @Test
    void getReceiptUrl_withReceiptUrlOnPaiement_returnsIt() {
        Paiement p = new Paiement();
        p.setReceiptUrl("https://receipt.stripe.com/r/test");

        Optional<String> result = stripeService.getReceiptUrl(p);

        assertTrue(result.isPresent());
        assertEquals("https://receipt.stripe.com/r/test", result.get());
    }

    @Test
    void getReceiptUrl_withNoReceiptUrlAndNoPaymentIntentId_returnsEmpty() {
        Paiement p = new Paiement();
        p.setReceiptUrl(null);
        p.setPaymentIntentId(null);

        Optional<String> result = stripeService.getReceiptUrl(p);

        assertTrue(result.isEmpty());
    }

    @Test
    void getReceiptUrl_withBlankReceiptUrlAndBlankPaymentIntentId_returnsEmpty() {
        Paiement p = new Paiement();
        p.setReceiptUrl("   ");
        p.setPaymentIntentId("   ");

        Optional<String> result = stripeService.getReceiptUrl(p);

        assertTrue(result.isEmpty());
    }

    @Test
    void getReceiptUrl_withPaidEcheanceButNoStripeKey_returnsEmpty() {
        Paiement p = new Paiement();
        p.setReceiptUrl(null);
        p.setPaymentIntentId(null);

        Echeance e = new Echeance();
        e.setStatut("paye");
        e.setReference("pi_test_123");
        e.setNumero(1);
        p.setEcheances(List.of(e));

        // Avec une clé dummy, l'appel Stripe échoue silencieusement → empty
        Optional<String> result = stripeService.getReceiptUrl(p);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReceiptUrl_piIdViaEcheancePayee_recupereReceiptUrlDeLaCharge() throws Exception {
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);
        chargeStatic = Mockito.mockStatic(Charge.class);

        Paiement p = new Paiement();
        p.setReceiptUrl(null);
        p.setPaymentIntentId(null);

        Echeance e = new Echeance();
        e.setStatut("paye");
        e.setReference("pi_test_123");
        e.setNumero(1);
        p.setEcheances(List.of(e));

        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getLatestCharge()).thenReturn("ch_1");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(pi);

        Charge charge = mock(Charge.class);
        when(charge.getReceiptUrl()).thenReturn("https://stripe.com/receipt/xyz");
        chargeStatic.when(() -> Charge.retrieve("ch_1")).thenReturn(charge);

        Optional<String> result = stripeService.getReceiptUrl(p);

        assertTrue(result.isPresent());
        assertEquals("https://stripe.com/receipt/xyz", result.get());
    }

    @Test
    void getReceiptUrl_viaPaymentIntentIdDirect_utiliseLatestChargeObjectEnFallback() throws Exception {
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);

        Paiement p = new Paiement();
        p.setReceiptUrl(null);
        p.setPaymentIntentId("pi_fallback");

        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getLatestCharge()).thenReturn(null);
        Charge latestCharge = mock(Charge.class);
        when(latestCharge.getReceiptUrl()).thenReturn("https://stripe.com/receipt/fallback");
        when(pi.getLatestChargeObject()).thenReturn(latestCharge);
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_fallback")).thenReturn(pi);

        Optional<String> result = stripeService.getReceiptUrl(p);

        assertTrue(result.isPresent());
        assertEquals("https://stripe.com/receipt/fallback", result.get());
    }

    @Test
    void getReceiptUrl_erreurStripe_retourneEmpty() throws Exception {
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);

        Paiement p = new Paiement();
        p.setPaymentIntentId("pi_error");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_error"))
                .thenThrow(new RuntimeException("stripe down"));

        Optional<String> result = stripeService.getReceiptUrl(p);

        assertTrue(result.isEmpty());
    }

    // ── createPaymentIntentWithMetadata — succes ───────────────────────────────

    @Test
    void createPaymentIntent_succes_retourneLePaymentIntentCree() throws Exception {
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);

        PaymentIntent created = mock(PaymentIntent.class);
        when(created.getId()).thenReturn("pi_new_123");
        paymentIntentStatic.when(() -> PaymentIntent.create(
                        org.mockito.ArgumentMatchers.any(com.stripe.param.PaymentIntentCreateParams.class),
                        org.mockito.ArgumentMatchers.any(com.stripe.net.RequestOptions.class)))
                .thenReturn(created);

        Map<String, Object> req = Map.of(
                "amount", 1500L,
                "currency", "eur",
                "metadata", Map.of("paiementId", "1")
        );

        PaymentIntent result = stripeService.createPaymentIntentWithMetadata(req, "idem-key-success");

        assertEquals("pi_new_123", result.getId());
    }

    @Test
    void createPaymentIntent_deviseParDefaut_utiliseeQuandNonFournie() throws Exception {
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);

        PaymentIntent created = mock(PaymentIntent.class);
        when(created.getId()).thenReturn("pi_default_currency");
        paymentIntentStatic.when(() -> PaymentIntent.create(
                        org.mockito.ArgumentMatchers.any(com.stripe.param.PaymentIntentCreateParams.class),
                        org.mockito.ArgumentMatchers.any(com.stripe.net.RequestOptions.class)))
                .thenReturn(created);

        Map<String, Object> req = Map.of("amount", 1000L);

        PaymentIntent result = stripeService.createPaymentIntentWithMetadata(req, "idem-key-default");

        assertEquals("pi_default_currency", result.getId());
    }

    // ── retrieveClientSecret ──────────────────────────────────────────────────

    @Test
    void retrieveClientSecret_withNullId_returnsNull() {
        assertNull(stripeService.retrieveClientSecret(null));
    }

    @Test
    void retrieveClientSecret_withBlankId_returnsNull() {
        assertNull(stripeService.retrieveClientSecret("   "));
    }

    @Test
    void retrieveClientSecret_succes_retourneLeClientSecret() throws Exception {
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);

        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getClientSecret()).thenReturn("secret_abc");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_cs_1")).thenReturn(pi);

        String result = stripeService.retrieveClientSecret("pi_cs_1");

        assertEquals("secret_abc", result);
    }

    @Test
    void retrieveClientSecret_erreurStripe_retourneNull() throws Exception {
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);

        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_error"))
                .thenThrow(new RuntimeException("stripe down"));

        assertNull(stripeService.retrieveClientSecret("pi_error"));
    }
}
