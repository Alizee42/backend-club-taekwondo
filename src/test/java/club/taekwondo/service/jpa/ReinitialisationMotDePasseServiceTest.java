package club.taekwondo.service.jpa;

import club.taekwondo.dto.ReinitialisationMotDePasseDTO;
import club.taekwondo.entity.jpa.ReinitialisationMotDePasse;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

class ReinitialisationMotDePasseServiceTest extends AbstractServiceIntegrationTest {

    @MockBean
    private EmailService emailService;

    @Autowired
    private ReinitialisationMotDePasseService service;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Utilisateur utilisateur;

    @BeforeEach
    void setupReset() {
        utilisateur = new Utilisateur();
        utilisateur.setNom("Testeur");
        utilisateur.setPrenom("Parent");
        utilisateur.setEmail("reset-test@test.com");
        utilisateur.setPassword(passwordEncoder.encode("ancien-mdp"));
        utilisateur.setRole(Role.PARENT);
        utilisateur = utilisateurRepository.save(utilisateur);
    }

    @Test
    void creerDemande_utilisateurExistant_genereUnTokenEtUneExpirationDansUneHeure() {
        ReinitialisationMotDePasseDTO dto = service.creerDemande(utilisateur.getId());

        assertNotNull(dto.getToken());
        assertFalse(dto.isUtilise());
        assertTrue(dto.getDateExpiration().isAfter(LocalDateTime.now().plusMinutes(50)));
        assertTrue(dto.getDateExpiration().isBefore(LocalDateTime.now().plusMinutes(70)));
    }

    @Test
    void creerDemande_utilisateurIntrouvable_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.creerDemande(999999L));
    }

    @Test
    void demanderReinitialisation_emailInconnu_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.demanderReinitialisation("inconnu@test.com"));
    }

    @Test
    void demanderReinitialisation_succes_persisteLaDemandeEtEnvoieLEmail() {
        service.demanderReinitialisation(utilisateur.getEmail());

        assertEquals(1, reinitialisationMotDePasseRepository.findAll().size());
        verify(emailService).envoyerEmailReinitialisationMotDePasse(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(utilisateur.getEmail()),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void demanderReinitialisation_emailEnErreur_neFaitPasEchouerLaDemande() {
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                .when(emailService).envoyerEmailReinitialisationMotDePasse(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        // Ne doit pas lever d'exception malgre l'echec de l'envoi email
        service.demanderReinitialisation(utilisateur.getEmail());

        assertEquals(1, reinitialisationMotDePasseRepository.findAll().size());
    }

    @Test
    void validerToken_tokenValide_retourneTrueEtMarqueUtilise() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken("token-valide");
        demande.setDateExpiration(LocalDateTime.now().plusHours(1));
        demande.setUtilise(false);
        reinitialisationMotDePasseRepository.save(demande);

        assertTrue(service.validerToken("token-valide"));
    }

    @Test
    void validerToken_tokenExpire_retourneFalse() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken("token-expire");
        demande.setDateExpiration(LocalDateTime.now().minusMinutes(5));
        demande.setUtilise(false);
        reinitialisationMotDePasseRepository.save(demande);

        assertFalse(service.validerToken("token-expire"));
    }

    @Test
    void validerToken_tokenDejaUtilise_retourneFalse() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken("token-utilise");
        demande.setDateExpiration(LocalDateTime.now().plusHours(1));
        demande.setUtilise(true);
        reinitialisationMotDePasseRepository.save(demande);

        assertFalse(service.validerToken("token-utilise"));
    }

    @Test
    void validerToken_tokenInconnu_retourneFalse() {
        assertFalse(service.validerToken("token-inexistant"));
    }

    @Test
    void reinitialiserMotDePasse_tokenManquant_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reinitialiserMotDePasse("", "nouveauMdp123"));
    }

    @Test
    void reinitialiserMotDePasse_motDePasseManquant_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reinitialiserMotDePasse("un-token", "  "));
    }

    @Test
    void reinitialiserMotDePasse_tokenValide_metAJourLeMotDePasseEtMarqueLeTokenUtilise() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken("token-reset-ok");
        demande.setDateExpiration(LocalDateTime.now().plusHours(1));
        demande.setUtilise(false);
        reinitialisationMotDePasseRepository.save(demande);

        boolean resultat = service.reinitialiserMotDePasse("token-reset-ok", "nouveauMotDePasse123");

        assertTrue(resultat);
        Utilisateur reloaded = utilisateurRepository.findById(utilisateur.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("nouveauMotDePasse123", reloaded.getPassword()));
        assertFalse(reloaded.isPasswordTemporaire());

        ReinitialisationMotDePasseDTO tokenReload = service.getByToken("token-reset-ok").orElseThrow();
        assertTrue(tokenReload.isUtilise());
    }

    @Test
    void reinitialiserMotDePasse_tokenExpire_retourneFalseEtNeChangeRien() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken("token-reset-expire");
        demande.setDateExpiration(LocalDateTime.now().minusMinutes(1));
        demande.setUtilise(false);
        reinitialisationMotDePasseRepository.save(demande);

        boolean resultat = service.reinitialiserMotDePasse("token-reset-expire", "nouveauMotDePasse123");

        assertFalse(resultat);
        Utilisateur reloaded = utilisateurRepository.findById(utilisateur.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("ancien-mdp", reloaded.getPassword()));
    }

    @Test
    void reinitialiserMotDePasse_tokenInconnu_retourneFalse() {
        assertFalse(service.reinitialiserMotDePasse("token-inconnu", "nouveauMotDePasse123"));
    }

    @Test
    void getByToken_tokenInconnu_retourneOptionalVide() {
        assertTrue(service.getByToken("nexiste-pas").isEmpty());
    }
}
