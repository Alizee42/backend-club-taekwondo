package club.taekwondo.service;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StripeServiceTest {

    private StripeService stripeService;

    @BeforeEach
    void setup() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", "sk_test_dummy");
        ReflectionTestUtils.setField(stripeService, "stripePublicKey", "pk_test_dummy");
        ReflectionTestUtils.setField(stripeService, "defaultCurrency", "eur");
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

    // ── retrieveClientSecret ──────────────────────────────────────────────────

    @Test
    void retrieveClientSecret_withNullId_returnsNull() {
        assertNull(stripeService.retrieveClientSecret(null));
    }

    @Test
    void retrieveClientSecret_withBlankId_returnsNull() {
        assertNull(stripeService.retrieveClientSecret("   "));
    }
}
