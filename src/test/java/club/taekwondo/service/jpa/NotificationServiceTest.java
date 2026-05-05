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
