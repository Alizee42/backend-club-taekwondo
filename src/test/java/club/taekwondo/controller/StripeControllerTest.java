package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.EmailService;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeControllerTest {

    @Mock
    private PaiementService paiementService;
    @Mock
    private PaiementAccessService paiementAccessService;
    @Mock
    private StripeService stripeService;
    @Mock
    private EmailService emailService;

    private StripeController controller;

    @BeforeEach
    void setUp() {
        controller = new StripeController(paiementService, paiementAccessService, stripeService, emailService);
    }

    /* ===================== /receipt/{paiementId} ===================== */

    @Test
    void receipt_paiementIntrouvable_returns400() {
        when(paiementService.getById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.redirectToStripeReceipt(99L, auth("user@test.com", "ROLE_PARENT"));

        // orElseThrow lance IllegalArgumentException attrapée par catch(Exception) → 400
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void receipt_ownershipDenied_returns400() {
        Paiement paiement = paiement(10L, 99L);
        when(paiementService.getById(10L)).thenReturn(Optional.of(paiement));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse"))
                .when(paiementAccessService).assertCanAccessPaiement(any(), eq(paiement));

        ResponseEntity<?> response = controller.redirectToStripeReceipt(10L, auth("other@test.com", "ROLE_PARENT"));

        // ResponseStatusException attrapée par catch(Exception) → 400
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paiementAccessService).assertCanAccessPaiement(any(), eq(paiement));
    }

    @Test
    void receipt_aucunRecuDisponible_returns404() {
        Paiement paiement = paiement(10L, 5L);
        when(paiementService.getById(10L)).thenReturn(Optional.of(paiement));
        when(stripeService.getReceiptUrl(paiement)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.redirectToStripeReceipt(10L, auth("user@test.com", "ROLE_PARENT"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void receipt_nominal_returnsReceiptUrl() {
        Paiement paiement = paiement(10L, 5L);
        when(paiementService.getById(10L)).thenReturn(Optional.of(paiement));
        when(stripeService.getReceiptUrl(paiement)).thenReturn(Optional.of("https://pay.stripe.com/receipt/abc"));

        ResponseEntity<?> response = controller.redirectToStripeReceipt(10L, auth("user@test.com", "ROLE_PARENT"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("https://pay.stripe.com/receipt/abc", body.get("receiptUrl"));
    }

    /* ===================== /payment-intent ===================== */

    @Test
    void createPaymentIntent_stripeNonConfigure_returns503() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1L), auth("user@test.com", "ROLE_PARENT"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_paiementIdManquant_returns400() {
        when(stripeService.isConfigured()).thenReturn(true);
        // requireAuthenticatedUser retourne null par défaut (mock) — utilisé plus loin seulement pour l'email
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(5L, "user@test.com"));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of(), auth("user@test.com", "ROLE_PARENT"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_paiementNonTrouve_returns404() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(5L, "user@test.com"));
        when(paiementService.getById(42L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 42), auth("user@test.com", "ROLE_PARENT"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_ownershipDenied_verifieLeCheck() {
        Paiement paiement = paiement(42L, 99L);
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(5L, "user@test.com"));
        when(paiementService.getById(42L)).thenReturn(Optional.of(paiement));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse"))
                .when(paiementAccessService).assertCanAccessPaiement(any(), eq(paiement));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 42), auth("user@test.com", "ROLE_PARENT"));

        // ResponseStatusException attrapée par catch(Exception) → 400
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paiementAccessService).assertCanAccessPaiement(any(), eq(paiement));
    }

    /* ===================== /sync-payment ===================== */

    @Test
    void syncPayment_paymentIntentIdManquant_returns400() {
        ResponseEntity<?> response = controller.syncPayment(Map.of(), auth("user@test.com", "ROLE_PARENT"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("paymentIntentId manquant", body.get("error"));
    }

    /* ===================== helpers ===================== */

    private Authentication auth(String email, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, authority);
        token.setAuthenticated(true);
        return token;
    }

    private Paiement paiement(Long id, Long utilisateurId) {
        Paiement p = new Paiement();
        p.setId(id);
        Utilisateur u = new Utilisateur();
        u.setId(utilisateurId);
        p.setUtilisateur(u);
        return p;
    }

    private Utilisateur utilisateur(Long id, String email) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        return u;
    }
}
