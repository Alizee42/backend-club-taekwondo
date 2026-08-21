package club.taekwondo.service.jpa;

import club.taekwondo.dto.NotificationDTO;
import club.taekwondo.entity.jpa.Notification;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @InjectMocks private NotificationService notificationService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setNom("Dupont");
        utilisateur.setPrenom("Marie");
        utilisateur.setEmail("marie@test.com");
    }

    // ── envoyerNotification ──────────────────────────────────────

    @Test
    void envoyerNotification_succes() {
        Notification saved = new Notification();
        saved.setId(10L);
        saved.setTitre("Nouvel événement");
        saved.setMessage("Tournoi ce samedi");
        saved.setType("evenement");
        saved.setLu(false);
        saved.setDateEnvoi(LocalDateTime.now());
        saved.setUtilisateur(utilisateur);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationDTO result = notificationService.envoyerNotification(1L, "Nouvel événement", "Tournoi ce samedi", "evenement");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Nouvel événement", result.getTitre());
        assertEquals("evenement", result.getType());
        assertFalse(result.isLu());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void envoyerNotification_utilisateurInexistant_throwsException() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            notificationService.envoyerNotification(99L, "Titre", "Message", "general")
        );

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void envoyerNotification_avecLienAction() {
        Notification saved = new Notification();
        saved.setId(11L);
        saved.setTitre("Paiement");
        saved.setMessage("Échéance due");
        saved.setType("paiement");
        saved.setLienAction("/membre/paiement");
        saved.setLu(false);
        saved.setDateEnvoi(LocalDateTime.now());
        saved.setUtilisateur(utilisateur);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationDTO result = notificationService.envoyerNotification(1L, "Paiement", "Échéance due", "paiement", "/membre/paiement");

        assertEquals("/membre/paiement", result.getLienAction());
    }

    // ── getToutesNotificationsUtilisateur ──────────────────────

    @Test
    void getToutesNotificationsUtilisateur_retourneListe() {
        Notification n1 = buildNotification(1L, "Titre A", "Msg A", false);
        Notification n2 = buildNotification(2L, "Titre B", "Msg B", true);

        when(notificationRepository.findByUtilisateurIdOrderByDateEnvoiDesc(1L))
            .thenReturn(List.of(n1, n2));

        List<NotificationDTO> result = notificationService.getToutesNotificationsUtilisateur(1L);

        assertEquals(2, result.size());
        assertEquals("Titre A", result.get(0).getTitre());
        assertFalse(result.get(0).isLu());
        assertTrue(result.get(1).isLu());
    }

    @Test
    void getToutesNotificationsUtilisateur_listeVide() {
        when(notificationRepository.findByUtilisateurIdOrderByDateEnvoiDesc(1L))
            .thenReturn(List.of());

        List<NotificationDTO> result = notificationService.getToutesNotificationsUtilisateur(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── marquerCommeLue ──────────────────────────────────────────

    @Test
    void marquerCommeLue_succes() {
        Notification notif = buildNotification(5L, "Test", "Message", false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notif);

        notificationService.marquerCommeLue(5L);

        assertTrue(notif.isLu());
        verify(notificationRepository).save(notif);
    }

    @Test
    void marquerCommeLue_notificationInexistante_throwsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            notificationService.marquerCommeLue(999L)
        );

        verify(notificationRepository, never()).save(any());
    }

    // ── marquerToutesCommeLues ───────────────────────────────────

    @Test
    void marquerToutesCommeLues_marqueToutes() {
        Notification n1 = buildNotification(1L, "A", "M1", false);
        Notification n2 = buildNotification(2L, "B", "M2", false);

        when(notificationRepository.findByUtilisateurIdAndLuFalse(1L)).thenReturn(List.of(n1, n2));

        notificationService.marquerToutesCommeLues(1L);

        assertTrue(n1.isLu());
        assertTrue(n2.isLu());
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }

    // ── deleteNotification ───────────────────────────────────────

    @Test
    void deleteNotification_succes() {
        Notification notif = buildNotification(7L, "Del", "Msg", false);
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(notif));

        notificationService.deleteNotification(7L);

        verify(notificationRepository).delete(notif);
    }

    @Test
    void deleteNotification_inexistante_throwsException() {
        when(notificationRepository.findById(888L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            notificationService.deleteNotification(888L)
        );

        verify(notificationRepository, never()).delete(any());
    }

    // ── envoyerNotification (surcharge courte) ───────────────────

    @Test
    void envoyerNotification_surchargeCourte_utiliseTitreParDefaut() {
        Notification saved = buildNotification(20L, "Notification", "Un message", false);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationDTO result = notificationService.envoyerNotification(1L, "Un message");

        assertEquals("Notification", result.getTitre());
    }

    // ── getOwnerId ────────────────────────────────────────────────

    @Test
    void getOwnerId_succes_retourneIdProprietaire() {
        Notification notif = buildNotification(5L, "Titre", "Msg", false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notif));

        Long ownerId = notificationService.getOwnerId(5L);

        assertEquals(1L, ownerId);
    }

    @Test
    void getOwnerId_notificationInexistante_throwsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> notificationService.getOwnerId(999L));
    }

    // ── envoyerNotificationATous ──────────────────────────────────

    @Test
    void envoyerNotificationATous_notifieChaqueUtilisateur() {
        Utilisateur u2 = new Utilisateur();
        u2.setId(2L);
        when(utilisateurRepository.findAll()).thenReturn(List.of(utilisateur, u2));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(u2));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.envoyerNotificationATous("Annonce", "Message global", "info");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    // ── envoyerNotificationAuxAdminsClub ──────────────────────────

    @Test
    void envoyerNotificationAuxAdminsClub_neNotifieQueLesAdminsDuClub() {
        club.taekwondo.entity.jpa.Club club10 = new club.taekwondo.entity.jpa.Club();
        club10.setId(10L);
        club.taekwondo.entity.jpa.Club club20 = new club.taekwondo.entity.jpa.Club();
        club20.setId(20L);

        Utilisateur adminClub10 = new Utilisateur();
        adminClub10.setId(2L);
        adminClub10.setRole(club.taekwondo.enums.Role.ADMIN);
        adminClub10.setClub(club10);

        Utilisateur adminClub20 = new Utilisateur();
        adminClub20.setId(3L);
        adminClub20.setRole(club.taekwondo.enums.Role.ADMIN);
        adminClub20.setClub(club20);

        Utilisateur membreClub10 = new Utilisateur();
        membreClub10.setId(4L);
        membreClub10.setRole(club.taekwondo.enums.Role.MEMBRE);
        membreClub10.setClub(club10);

        when(utilisateurRepository.findAll()).thenReturn(List.of(adminClub10, adminClub20, membreClub10));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(adminClub10));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.envoyerNotificationAuxAdminsClub(10L, "Nouveau paiement", "Message", "paiement", "/admin/paiements");

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(utilisateurRepository).findById(2L);
        verify(utilisateurRepository, never()).findById(3L);
        verify(utilisateurRepository, never()).findById(4L);
    }

    // ── envoyerNotificationAuxSuperAdmins ──────────────────────────

    @Test
    void envoyerNotificationAuxSuperAdmins_neNotifieQueLesSuperAdmins() {
        Utilisateur superAdmin = new Utilisateur();
        superAdmin.setId(5L);
        superAdmin.setRole(club.taekwondo.enums.Role.SUPER_ADMIN);

        Utilisateur admin = new Utilisateur();
        admin.setId(6L);
        admin.setRole(club.taekwondo.enums.Role.ADMIN);

        when(utilisateurRepository.findAll()).thenReturn(List.of(superAdmin, admin));
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(superAdmin));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.envoyerNotificationAuxSuperAdmins("Nouvel admin", "Message", "utilisateur", "/super-admin/utilisateurs");

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(utilisateurRepository).findById(5L);
        verify(utilisateurRepository, never()).findById(6L);
    }

    // ── helpers ──────────────────────────────────────────────────

    private Notification buildNotification(Long id, String titre, String message, boolean lu) {
        Notification n = new Notification();
        n.setId(id);
        n.setTitre(titre);
        n.setMessage(message);
        n.setType("general");
        n.setLu(lu);
        n.setDateEnvoi(LocalDateTime.now());
        n.setUtilisateur(utilisateur);
        return n;
    }
}
