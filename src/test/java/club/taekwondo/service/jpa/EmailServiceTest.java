package club.taekwondo.service.jpa;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSenderImpl mailSender;

    @Mock
    private ClubMailSenderFactory clubMailSenderFactory;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "clubMailSenderFactory", clubMailSenderFactory);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@club-taekwondo.com");
        ReflectionTestUtils.setField(emailService, "mailUsername", "test@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://frontend.example.com");
        ReflectionTestUtils.setField(emailService, "contactTo", "contact@club-taekwondo.com");
        // Aucun club dans ces tests : Mockito retourne Optional.empty() par defaut pour
        // forClub() (non stubbe), ce qui fait retomber le service sur mailSender global.
    }

    private MimeMessage realMimeMessage() {
        // Un vrai MimeMessage est necessaire pour que MimeMessageHelper puisse l'ecrire.
        return new JavaMailSenderImpl().createMimeMessage();
    }

    @Test
    void envoyerEmailHtml_usernameNonConfigure_neTentePasLenvoi() {
        ReflectionTestUtils.setField(emailService, "mailUsername", "");

        emailService.envoyerEmailHtml("dest@test.com", "Sujet", "<p>Contenu</p>");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void envoyerEmailHtml_usernameNull_neTentePasLenvoi() {
        ReflectionTestUtils.setField(emailService, "mailUsername", null);

        emailService.envoyerEmailHtml("dest@test.com", "Sujet", "<p>Contenu</p>");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void envoyerEmailHtml_succes_appelleSendSurLeMailSender() {
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage());

        emailService.envoyerEmailHtml("dest@test.com", "Sujet", "<p>Contenu</p>");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void envoyerEmailHtml_echecEnvoiSmtp_estPropageCarSeuleMessagingExceptionEstCapturee() {
        // Le code catche MessagingException (erreur de construction du message),
        // mais mailSender.send() leve MailSendException (une RuntimeException) en cas
        // d'echec reseau/SMTP reel : cette exception n'est donc PAS absorbee malgre le
        // commentaire du code indiquant que l'echec d'envoi ne doit jamais faire
        // echouer l'appelant. Documente ce comportement reel plutot que l'intention.
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage());
        doThrow(new org.springframework.mail.MailSendException("SMTP down"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(org.springframework.mail.MailSendException.class, () ->
                emailService.envoyerEmailHtml("dest@test.com", "Sujet", "<p>Contenu</p>"));
    }

    @Test
    void envoyerEmailReinitialisationMotDePasse_declencheUnEnvoi() {
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage());

        emailService.envoyerEmailReinitialisationMotDePasse("dest@test.com", "token-abc");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void envoyerEmailConfirmationInscription_declencheUnEnvoi() {
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage());

        emailService.envoyerEmailConfirmationInscription("dest@test.com", "Alice");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void envoyerRecuPaiement_declencheUnEnvoi() {
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage());

        emailService.envoyerRecuPaiement("dest@test.com", 100.0, "Cotisation", "https://stripe.com/receipt/123");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void envoyerMessageContact_envoieAuClubEtAccuseReceptionAUtilisateur() {
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage(), realMimeMessage());

        emailService.envoyerMessageContact("Jean Dupont", "jean@test.com", "Question", "Bonjour, ...");

        verify(mailSender, org.mockito.Mockito.times(2)).send(any(MimeMessage.class));
    }

    @Test
    void envoyerMessageContact_contactToVide_utiliseFromEmailCommeDestinataireClub() {
        ReflectionTestUtils.setField(emailService, "contactTo", "");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage(), realMimeMessage());

        assertDoesNotThrow(() ->
                emailService.envoyerMessageContact("Jean", "jean@test.com", "Objet", "Contenu"));

        verify(mailSender, org.mockito.Mockito.times(2)).send(any(MimeMessage.class));
    }

    @Test
    void envoyerMessageContact_echecAccuseReception_neLevePasException() {
        when(mailSender.createMimeMessage())
                .thenReturn(realMimeMessage())
                .thenThrow(new RuntimeException("Erreur creation message"));

        assertDoesNotThrow(() ->
                emailService.envoyerMessageContact("Jean", "jean@test.com", "Objet", "Contenu"));
    }
}
