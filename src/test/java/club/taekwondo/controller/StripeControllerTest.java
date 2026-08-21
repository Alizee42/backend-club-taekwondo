package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.EmailService;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import com.stripe.exception.IdempotencyException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    private MockedStatic<PaymentIntent> paymentIntentStatic;
    private MockedStatic<Charge> chargeStatic;

    @BeforeEach
    void setUp() {
        controller = new StripeController(paiementService, paiementAccessService, stripeService, emailService);
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);
        chargeStatic = Mockito.mockStatic(Charge.class);
    }

    @AfterEach
    void tearDown() {
        paymentIntentStatic.close();
        chargeStatic.close();
    }

    private Authentication auth() {
        return new TestingAuthenticationToken("u@test.com", null, "ROLE_MEMBRE");
    }

    private Utilisateur utilisateur(Long id, String email) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private Paiement paiementUnique(Long id, double montantTotal) {
        Paiement p = new Paiement();
        p.setId(id);
        p.setType("UNIQUE");
        p.setMontantTotal(montantTotal);
        return p;
    }

    // ---- getPublicKey ----

    @Test
    void getPublicKey_nonConfiguree_retourneNoContent() {
        ResponseEntity<?> response = controller.getPublicKey();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getPublicKey_valeurDummy_retourneNoContent() {
        ReflectionTestUtils.setField(controller, "publishableKey", "pk_dummy_123");

        ResponseEntity<?> response = controller.getPublicKey();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getPublicKey_configuree_retourneOk() {
        ReflectionTestUtils.setField(controller, "publishableKey", "pk_live_123");

        ResponseEntity<?> response = controller.getPublicKey();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- getConfigStatus ----

    @Test
    void getConfigStatus_delegueAuxDeuxSources() {
        ReflectionTestUtils.setField(controller, "publishableKey", "pk_live_123");
        when(stripeService.isConfigured()).thenReturn(true);

        ResponseEntity<?> response = controller.getConfigStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("publishableKeyConfigured"));
        assertEquals(true, body.get("secretKeyConfigured"));
    }

    // ---- createPaymentIntent ----

    @Test
    void createPaymentIntent_stripeNonConfigure_retourneServiceUnavailable() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_sansPaiementId_retourneBadRequest() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of(), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_paiementIntrouvable_retourneNotFound() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        when(paiementService.getById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 99), auth());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_montantInvalide_retourneBadRequest() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Paiement p = paiementUnique(1L, 0.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_succesUnique_retourneClientSecret() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Paiement p = paiementUnique(1L, 100.0);
        p.setUtilisateur(utilisateur(1L, "u@test.com"));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        PaymentIntent createdPi = mock(PaymentIntent.class);
        when(createdPi.getClientSecret()).thenReturn("secret_abc");
        when(createdPi.getId()).thenReturn("pi_123");
        when(stripeService.createPaymentIntentWithMetadata(any(), anyString())).thenReturn(createdPi);

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("secret_abc", body.get("clientSecret"));
    }

    @Test
    void createPaymentIntent_echelonneSansEcheances_retourneBadRequest() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setEcheances(List.of());
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_echelonneEcheanceInexistanteDemandee_retourneBadRequest() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("en attente");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1, "echeanceId", 999), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_echelonneToutesPayees_retourneBadRequest() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("payé");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_echelonneSucces_retourneClientSecret() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("en attente");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        PaymentIntent createdPi = mock(PaymentIntent.class);
        when(createdPi.getClientSecret()).thenReturn("secret_ech");
        when(createdPi.getId()).thenReturn("pi_ech_123");
        when(stripeService.createPaymentIntentWithMetadata(any(), anyString())).thenReturn(createdPi);

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_reutilisePIConfirmableExistant() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("en attente");
        e.setReference("pi_existing");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        PaymentIntent existingPi = mock(PaymentIntent.class);
        when(existingPi.getStatus()).thenReturn("requires_payment_method");
        when(existingPi.getClientSecret()).thenReturn("secret_reused");
        when(existingPi.getId()).thenReturn("pi_existing");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_existing")).thenReturn(existingPi);

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("secret_reused", body.get("clientSecret"));
    }

    @Test
    void createPaymentIntent_idempotencyException_retryAvecNouvelleCle() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(utilisateur(1L, "u@test.com"));
        Paiement p = paiementUnique(1L, 100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        PaymentIntent createdPi = mock(PaymentIntent.class);
        when(createdPi.getClientSecret()).thenReturn("secret_retry");
        when(createdPi.getId()).thenReturn("pi_retry");
        IdempotencyException idemEx = mock(IdempotencyException.class);
        when(stripeService.createPaymentIntentWithMetadata(any(), anyString()))
                .thenThrow(idemEx)
                .thenReturn(createdPi);

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createPaymentIntent_exceptionInattendue_retourneBadRequest() {
        when(stripeService.isConfigured()).thenReturn(true);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenThrow(new RuntimeException("non authentifie"));

        ResponseEntity<?> response = controller.createPaymentIntent(Map.of("paiementId", 1), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- syncPayment ----

    @Test
    void syncPayment_sansPaymentIntentId_retourneBadRequest() {
        ResponseEntity<?> response = controller.syncPayment(Map.of(), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void syncPayment_piNonSucceeded_retourneBadRequest() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("requires_action");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void syncPayment_metadataManquante_retourneBadRequest() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("succeeded");
        when(pi.getMetadata()).thenReturn(null);
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void syncPayment_paiementIntrouvable_retourneNotFound() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("succeeded");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "99"));
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);
        when(paiementService.getById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void syncPayment_dejaPayeSansRestant_retourneAlreadyPaid() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("succeeded");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1", "sendReceiptEmail", "false"));
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("payé");
        p.setMontantRestant(0.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("already-paid", body.get("status"));
    }

    @Test
    void syncPayment_paiementUnique_marquePayeSansEmail() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("succeeded");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1", "sendReceiptEmail", "false"));
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("en attente");
        p.setMontantRestant(100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("payé", p.getStatut());
        assertEquals(0.0, p.getMontantRestant());
    }

    @Test
    void syncPayment_avecRecuEtEmail_envoieEmail() throws Exception {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("succeeded");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1", "sendReceiptEmail", "true"));
        when(pi.getLatestCharge()).thenReturn("ch_1");
        when(pi.getReceiptEmail()).thenReturn("client@test.com");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);

        Charge charge = mock(Charge.class);
        when(charge.getReceiptUrl()).thenReturn("https://stripe.com/receipt/1");
        chargeStatic.when(() -> Charge.retrieve("ch_1")).thenReturn(charge);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("en attente");
        p.setMontantRestant(100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void syncPayment_echelonneMarqueEcheancePayee() {
        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("en attente");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setStatut("en attente");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("succeeded");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1", "echeanceId", "5", "sendReceiptEmail", "false"));
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("payé", e.getStatut());
    }

    @Test
    void syncPayment_echelonneAucuneImpayee_retourneNoUnpaidInstallment() {
        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("payé");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setStatut("en attente");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getStatus()).thenReturn("succeeded");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1", "sendReceiptEmail", "false"));
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(pi);

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("no-unpaid-installment", body.get("status"));
    }

    @Test
    void syncPayment_exceptionInattendue_retourneBadRequest() {
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_1")).thenThrow(new RuntimeException("erreur stripe"));

        ResponseEntity<?> response = controller.syncPayment(Map.of("paymentIntentId", "pi_1"), auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- redirectToStripeReceipt ----

    @Test
    void redirectToStripeReceipt_paiementIntrouvable_retourneBadRequest() {
        when(paiementService.getById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.redirectToStripeReceipt(1L, auth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void redirectToStripeReceipt_aucunRecu_retourneNotFound() {
        Paiement p = paiementUnique(1L, 100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));
        when(stripeService.getReceiptUrl(p)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.redirectToStripeReceipt(1L, auth());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void redirectToStripeReceipt_succes_retourneUrl() {
        Paiement p = paiementUnique(1L, 100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));
        when(stripeService.getReceiptUrl(p)).thenReturn(Optional.of("https://stripe.com/receipt/1"));

        ResponseEntity<?> response = controller.redirectToStripeReceipt(1L, auth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void redirectToStripeReceipt_authAnonyme_neVerifiePasLAcces() {
        Paiement p = paiementUnique(1L, 100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));
        when(stripeService.getReceiptUrl(p)).thenReturn(Optional.of("https://stripe.com/receipt/1"));

        Authentication anonAuth = new TestingAuthenticationToken("anonymousUser", null, "ROLE_ANONYMOUS");

        ResponseEntity<?> response = controller.redirectToStripeReceipt(1L, anonAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
