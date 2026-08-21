package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.EcheanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcheanceReminderJobTest {

    @Mock
    private EcheanceRepository echeanceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    private EcheanceReminderJob job;

    @BeforeEach
    void setUp() {
        job = new EcheanceReminderJob();
        org.springframework.test.util.ReflectionTestUtils.setField(job, "echeanceRepository", echeanceRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(job, "notificationService", notificationService);
        org.springframework.test.util.ReflectionTestUtils.setField(job, "emailService", emailService);
    }

    private Utilisateur utilisateur(Long id, String email, String prenom, Role role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        u.setPrenom(prenom);
        u.setRole(role);
        return u;
    }

    private Echeance echeanceAvecPaiement(Paiement paiement) {
        Echeance e = new Echeance();
        e.setId(1L);
        e.setMontant(50.0);
        e.setDateEcheance(LocalDate.now().minusDays(5));
        e.setPaiement(paiement);
        return e;
    }

    @Test
    void rappelerEcheancesEnRetard_aucuneEcheance_neFaitAucunAppel() {
        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of());

        job.rappelerEcheancesEnRetard();

        verify(notificationService, never()).envoyerNotification(anyLong(), anyString(), anyString(), anyString(), anyString());
        verify(emailService, never()).envoyerEmailHtml(any(), anyString(), anyString(), anyString());
    }

    @Test
    void rappelerEcheancesEnRetard_echeanceSansPaiement_estIgnoree() {
        Echeance sansPaiement = new Echeance();
        sansPaiement.setId(2L);
        sansPaiement.setPaiement(null);
        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(sansPaiement));

        job.rappelerEcheancesEnRetard();

        verify(notificationService, never()).envoyerNotification(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rappelerEcheancesEnRetard_utilisateurDirectSurLePaiement_recoitNotifEtEmail() {
        Utilisateur parent = utilisateur(1L, "parent@test.com", "Jean", Role.PARENT);
        Paiement paiement = new Paiement();
        paiement.setUtilisateur(parent);
        Echeance echeance = echeanceAvecPaiement(paiement);

        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(echeance));

        job.rappelerEcheancesEnRetard();

        verify(notificationService).envoyerNotification(
                eq(1L), eq("Échéance en retard"), anyString(), eq("paiement"), eq("/parent/paiements"));
        verify(emailService).envoyerEmailHtml(any(), eq("parent@test.com"), anyString(), anyString());
    }

    @Test
    void rappelerEcheancesEnRetard_membreSansUtilisateurDirect_utiliseCompteUtilisateurDuMembre() {
        Utilisateur compteMembre = utilisateur(2L, "membre@test.com", "Alice", Role.MEMBRE);
        Membre membre = new Membre();
        membre.setCompteUtilisateur(compteMembre);

        Paiement paiement = new Paiement();
        paiement.setUtilisateur(null);
        paiement.setMembre(membre);
        Echeance echeance = echeanceAvecPaiement(paiement);

        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(echeance));

        job.rappelerEcheancesEnRetard();

        verify(notificationService).envoyerNotification(
                eq(2L), anyString(), anyString(), anyString(), eq("/membre/paiements"));
    }

    @Test
    void rappelerEcheancesEnRetard_membreSansCompteUtilise_leParentDuMembreEnFallback() {
        Utilisateur parentDuMembre = utilisateur(3L, "parent-membre@test.com", "Paul", Role.PARENT);
        Membre membre = new Membre();
        membre.setCompteUtilisateur(null);
        membre.setParent(parentDuMembre);

        Paiement paiement = new Paiement();
        paiement.setUtilisateur(null);
        paiement.setMembre(membre);
        Echeance echeance = echeanceAvecPaiement(paiement);

        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(echeance));

        job.rappelerEcheancesEnRetard();

        verify(notificationService).envoyerNotification(
                eq(3L), anyString(), anyString(), anyString(), eq("/parent/paiements"));
    }

    @Test
    void rappelerEcheancesEnRetard_aucunDestinataireResolvable_neFaitAucunAppel() {
        Paiement paiement = new Paiement();
        paiement.setUtilisateur(null);
        paiement.setMembre(null);
        Echeance echeance = echeanceAvecPaiement(paiement);

        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(echeance));

        job.rappelerEcheancesEnRetard();

        verify(notificationService, never()).envoyerNotification(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rappelerEcheancesEnRetard_emailVide_neTentePasLenvoiEmailMaisNotifieQuandMeme() {
        Utilisateur parent = utilisateur(4L, "", "Marc", Role.PARENT);
        Paiement paiement = new Paiement();
        paiement.setUtilisateur(parent);
        Echeance echeance = echeanceAvecPaiement(paiement);

        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(echeance));

        job.rappelerEcheancesEnRetard();

        verify(notificationService).envoyerNotification(eq(4L), anyString(), anyString(), anyString(), anyString());
        verify(emailService, never()).envoyerEmailHtml(any(), anyString(), anyString(), anyString());
    }

    @Test
    void rappelerEcheancesEnRetard_echecEnvoiEmail_neBloquePasLeTraitement() {
        Utilisateur parent = utilisateur(5L, "parent@test.com", "Sophie", Role.PARENT);
        Paiement paiement = new Paiement();
        paiement.setUtilisateur(parent);
        Echeance echeance = echeanceAvecPaiement(paiement);

        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(echeance));
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .envoyerEmailHtml(any(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> job.rappelerEcheancesEnRetard());

        verify(notificationService).envoyerNotification(eq(5L), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rappelerEcheancesEnRetard_exceptionSurUneEcheance_neBloquePasLesAutres() {
        Utilisateur parent1 = utilisateur(6L, "p1@test.com", "P1", Role.PARENT);
        Paiement paiementEnErreur = new Paiement();
        paiementEnErreur.setUtilisateur(parent1);
        Echeance echeanceEnErreur = echeanceAvecPaiement(paiementEnErreur);
        echeanceEnErreur.setId(10L);

        Utilisateur parent2 = utilisateur(7L, "p2@test.com", "P2", Role.PARENT);
        Paiement paiementOk = new Paiement();
        paiementOk.setUtilisateur(parent2);
        Echeance echeanceOk = echeanceAvecPaiement(paiementOk);
        echeanceOk.setId(11L);

        when(echeanceRepository.findByStatutAndDateEcheanceBefore(eq("en attente"), any(LocalDate.class)))
                .thenReturn(List.of(echeanceEnErreur, echeanceOk));
        doThrow(new RuntimeException("Erreur notif"))
                .when(notificationService).envoyerNotification(eq(6L), anyString(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> job.rappelerEcheancesEnRetard());

        verify(notificationService).envoyerNotification(eq(7L), anyString(), anyString(), anyString(), anyString());
    }
}
