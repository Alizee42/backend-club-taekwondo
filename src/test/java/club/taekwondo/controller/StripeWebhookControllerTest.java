package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    @Mock
    private PaiementService paiementService;

    @Mock
    private Environment environment;

    private StripeWebhookController controller;

    private MockedStatic<Webhook> webhookStatic;
    private MockedStatic<PaymentIntent> paymentIntentStatic;
    private MockedStatic<Charge> chargeStatic;

    @BeforeEach
    void setUp() {
        controller = new StripeWebhookController(paiementService, environment);
        ReflectionTestUtils.setField(controller, "endpointSecret", "whsec_test");
        webhookStatic = Mockito.mockStatic(Webhook.class);
        paymentIntentStatic = Mockito.mockStatic(PaymentIntent.class);
        chargeStatic = Mockito.mockStatic(Charge.class);
    }

    @AfterEach
    void tearDown() {
        webhookStatic.close();
        paymentIntentStatic.close();
        chargeStatic.close();
    }

    private Event mockEvent(String type, PaymentIntent dataObject) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_123");
        when(event.getType()).thenReturn(type);
        when(event.getLivemode()).thenReturn(false);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.ofNullable(dataObject));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private Paiement paiementUnique(Long id, double montantTotal) {
        Paiement p = new Paiement();
        p.setId(id);
        p.setType("UNIQUE");
        p.setMontantTotal(montantTotal);
        return p;
    }

    // ---- validateWebhookSecret ----

    @Test
    void validateWebhookSecret_secretPresent_neLevePas() {
        controller.validateWebhookSecret();
    }

    @Test
    void validateWebhookSecret_secretDummyEnDev_loggeMaisNeLevePas() {
        ReflectionTestUtils.setField(controller, "endpointSecret", "whsec_dummy");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        controller.validateWebhookSecret();
    }

    @Test
    void validateWebhookSecret_secretAbsentEnProd_leveIllegalState() {
        ReflectionTestUtils.setField(controller, "endpointSecret", null);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"docker"});

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                controller::validateWebhookSecret);
    }

    // ---- handleWebhook ----

    @Test
    void handleWebhook_secretAbsent_retourneInternalServerError() {
        ReflectionTestUtils.setField(controller, "endpointSecret", "");

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void handleWebhook_signatureInvalide_retourneBadRequest() throws Exception {
        SignatureVerificationException ex = mock(SignatureVerificationException.class);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenThrow(ex);

        ResponseEntity<String> response = controller.handleWebhook("{}", "bad-sig");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleWebhook_impossibleDeserialiser_retourneOkIgnored() {
        Event event = mockEvent("payment_intent.succeeded", null);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        ResponseEntity<String> response = controller.handleWebhook("{\"data\":{\"object\":{}}}", "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ignored", response.getBody());
    }

    @Test
    void handleWebhook_succeededSansPaiementIdEnMetadata_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of());
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-paiementId", response.getBody());
    }

    @Test
    void handleWebhook_succeededPaiementIdInvalide_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "abc"));
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("invalid-paiementId", response.getBody());
    }

    @Test
    void handleWebhook_succeededPaiementIntrouvable_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "99"));
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);
        when(paiementService.getById(99L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("paiement-not-found", response.getBody());
    }

    @Test
    void handleWebhook_succeededDejaSolde_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("payé");
        p.setMontantRestant(0.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("already-paid", response.getBody());
    }

    @Test
    void handleWebhook_deviseIncorrecte_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("usd");
        when(pi.getAmountReceived()).thenReturn(10000L);
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("en attente");
        p.setMontantRestant(100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("amount-currency-mismatch", response.getBody());
    }

    @Test
    void handleWebhook_montantIncorrectUnique_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(5000L);
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("en attente");
        p.setMontantRestant(100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("amount-mismatch", response.getBody());
    }

    @Test
    void handleWebhook_succeededUnique_soldeLePaiement() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(10000L);
        when(pi.getLatestCharge()).thenReturn(null);
        when(pi.getStatus()).thenReturn("succeeded");
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("en attente");
        p.setMontantRestant(100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("payé", p.getStatut());
        assertEquals(0.0, p.getMontantRestant());
    }

    @Test
    void handleWebhook_succeededUniqueAvecCharge_persisteReceiptUrl() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(10000L);
        when(pi.getLatestCharge()).thenReturn("ch_1");
        when(pi.getStatus()).thenReturn("succeeded");
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Charge charge = mock(Charge.class);
        when(charge.getReceiptUrl()).thenReturn("https://stripe.com/receipt/1");
        chargeStatic.when(() -> Charge.retrieve("ch_1")).thenReturn(charge);

        Paiement p = paiementUnique(1L, 100.0);
        p.setStatut("en attente");
        p.setMontantRestant(100.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://stripe.com/receipt/1", p.getReceiptUrl());
    }

    @Test
    void handleWebhook_succeededEchelonneAucuneEcheanceCible_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(5000L);
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("payé");
        e.setReference("pi_autre");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setStatut("en attente");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("no-ech-found", response.getBody());
    }

    @Test
    void handleWebhook_succeededEchelonneMontantIncorrect_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(2000L);
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("en attente");
        e.setReference("pi_1");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setStatut("en attente");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("amount-mismatch", response.getBody());
    }

    @Test
    void handleWebhook_succeededEchelonneDejaPayee_retourneOk() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(5000L);
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Echeance e = new Echeance();
        e.setId(5L);
        e.setNumero(1);
        e.setMontant(50.0);
        e.setStatut("payé");
        e.setReference("pi_1");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setStatut("en attente");
        p.setEcheances(List.of(e));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("installment-already-paid", response.getBody());
    }

    @Test
    void handleWebhook_succeededEchelonneSoldeUneEcheance() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(5000L);
        when(pi.getLatestCharge()).thenReturn(null);
        when(pi.getStatus()).thenReturn("succeeded");
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Echeance e1 = new Echeance();
        e1.setId(5L);
        e1.setNumero(1);
        e1.setMontant(50.0);
        e1.setStatut("en attente");
        e1.setReference("pi_1");
        Echeance e2 = new Echeance();
        e2.setId(6L);
        e2.setNumero(2);
        e2.setMontant(50.0);
        e2.setStatut("en attente");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setStatut("en attente");
        p.setEcheances(List.of(e1, e2));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ok", response.getBody());
        assertEquals("payé", e1.getStatut());
        assertEquals("en attente", p.getStatut());
        assertEquals(1, p.getEcheancesRestantes());
    }

    @Test
    void handleWebhook_succeededEchelonneDerniereEcheance_soldeLePaiement() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of("paiementId", "1"));
        when(pi.getCurrency()).thenReturn("eur");
        when(pi.getAmountReceived()).thenReturn(5000L);
        when(pi.getLatestCharge()).thenReturn(null);
        when(pi.getStatus()).thenReturn("succeeded");
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        Echeance e1 = new Echeance();
        e1.setId(5L);
        e1.setNumero(1);
        e1.setMontant(50.0);
        e1.setStatut("en attente");
        e1.setReference("pi_1");
        Paiement p = new Paiement();
        p.setId(1L);
        p.setType("ECHELONNE");
        p.setStatut("en attente");
        p.setEcheances(List.of(e1));
        when(paiementService.getById(1L)).thenReturn(Optional.of(p));

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals("payé", p.getStatut());
        assertEquals(0, p.getEcheancesRestantes());
    }

    @Test
    void handleWebhook_paymentFailed_loggeSansErreur() {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_1");
        when(pi.getMetadata()).thenReturn(Map.of());
        Event event = mockEvent("payment_intent.payment_failed", pi);
        webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);

        ResponseEntity<String> response = controller.handleWebhook("{}", "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Webhook recu", response.getBody());
    }
}
