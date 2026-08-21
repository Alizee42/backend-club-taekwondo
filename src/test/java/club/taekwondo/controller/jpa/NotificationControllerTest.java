package club.taekwondo.controller.jpa;

import club.taekwondo.dto.NotificationDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.service.jpa.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @Mock private UtilisateurRepository utilisateurRepository;
    @InjectMocks private NotificationController controller;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setEmail("membre@test.com");
    }

    // ── GET /api/notifications ──────────────────────────────────

    @Test
    void getNotifications_sansAuth_retourne401() {
        ResponseEntity<List<NotificationDTO>> response = controller.getNotifications(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(notificationService, never()).getToutesNotificationsUtilisateur(anyLong());
    }

    @Test
    void getNotifications_avecAuth_retourneListe() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);

        NotificationDTO dto = buildDTO(1L, "Titre", "Message", false);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getToutesNotificationsUtilisateur(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<NotificationDTO>> response = controller.getNotifications(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Titre", response.getBody().get(0).getTitre());
    }

    // ── PUT /api/notifications/{id}/read ───────────────────────

    @Test
    void markAsRead_succes_retourne200() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(5L)).thenReturn(1L);
        doNothing().when(notificationService).marquerCommeLue(5L);

        ResponseEntity<?> response = controller.markAsRead(5L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).marquerCommeLue(5L);
    }

    @Test
    void markAsRead_notificationInexistante_retourne404() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(999L)).thenReturn(1L);
        doThrow(new IllegalArgumentException("Not found")).when(notificationService).marquerCommeLue(999L);

        ResponseEntity<?> response = controller.markAsRead(999L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void markAsRead_pasProprietaire_retourne403() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(5L)).thenReturn(42L);

        ResponseEntity<?> response = controller.markAsRead(5L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(notificationService, never()).marquerCommeLue(anyLong());
    }

    // ── PUT /api/notifications/mark-all-read ───────────────────

    @Test
    void markAllAsRead_sansAuth_retourne401() {
        ResponseEntity<?> response = controller.markAllAsRead(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(notificationService, never()).marquerToutesCommeLues(anyLong());
    }

    @Test
    void markAllAsRead_avecAuth_retourne200() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);

        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        doNothing().when(notificationService).marquerToutesCommeLues(1L);

        ResponseEntity<?> response = controller.markAllAsRead(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).marquerToutesCommeLues(1L);
    }

    // ── DELETE /api/notifications/{id} ─────────────────────────

    @Test
    void deleteNotification_succes_retourne200() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(3L)).thenReturn(1L);
        doNothing().when(notificationService).deleteNotification(3L);

        ResponseEntity<?> response = controller.deleteNotification(3L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).deleteNotification(3L);
    }

    @Test
    void deleteNotification_inexistante_retourne404() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(888L)).thenReturn(1L);
        doThrow(new IllegalArgumentException("Not found")).when(notificationService).deleteNotification(888L);

        ResponseEntity<?> response = controller.deleteNotification(888L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteNotification_pasProprietaire_retourne403() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(3L)).thenReturn(42L);

        ResponseEntity<?> response = controller.deleteNotification(3L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(notificationService, never()).deleteNotification(anyLong());
    }

    // ── POST /api/notifications/envoyer ────────────────────────

    @Test
    void envoyerNotification_succes_retourne200() {
        NotificationDTO dto = buildDTO(1L, "Titre", "Message", false);
        when(notificationService.envoyerNotification(1L, "Message")).thenReturn(dto);

        ResponseEntity<?> response = controller.envoyerNotification(1L, "Message");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void envoyerNotification_utilisateurInexistant_retourneBadRequest() {
        when(notificationService.envoyerNotification(999L, "Message"))
                .thenThrow(new IllegalArgumentException("Utilisateur introuvable"));

        ResponseEntity<?> response = controller.envoyerNotification(999L, "Message");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── GET /api/notifications/utilisateur/{id} ────────────────

    @Test
    void getNotificationsUtilisateur_sansAuth_retourneUnauthorized() {
        ResponseEntity<List<NotificationDTO>> response = controller.getNotificationsUtilisateur(1L, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getNotificationsUtilisateur_admin_accedeAuxNotificationsDeNimporteQui() {
        Authentication auth = new TestingAuthenticationToken("admin@test.com", null, "ROLE_ADMIN");
        auth.setAuthenticated(true);
        when(notificationService.getToutesNotificationsUtilisateur(5L)).thenReturn(List.of());

        ResponseEntity<List<NotificationDTO>> response = controller.getNotificationsUtilisateur(5L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(utilisateurRepository, never()).findByEmail(anyString());
    }

    @Test
    void getNotificationsUtilisateur_membrePourSoiMeme_retourneOk() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getToutesNotificationsUtilisateur(1L)).thenReturn(List.of());

        ResponseEntity<List<NotificationDTO>> response = controller.getNotificationsUtilisateur(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getNotificationsUtilisateur_membrePourAutrui_retourneForbidden() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));

        ResponseEntity<List<NotificationDTO>> response = controller.getNotificationsUtilisateur(999L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getNotificationsUtilisateur_appelantIntrouvable_retourneForbidden() {
        Authentication auth = new TestingAuthenticationToken("inconnu@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        ResponseEntity<List<NotificationDTO>> response = controller.getNotificationsUtilisateur(1L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── PUT /api/notifications/{id}/lue ────────────────────────

    @Test
    void marquerCommeLue_succes_retourne200() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(5L)).thenReturn(1L);

        ResponseEntity<?> response = controller.marquerCommeLue(5L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).marquerCommeLue(5L);
    }

    @Test
    void marquerCommeLue_notificationInexistante_retourne404() {
        Authentication auth = new TestingAuthenticationToken("membre@test.com", null, "ROLE_MEMBRE");
        auth.setAuthenticated(true);
        when(utilisateurRepository.findByEmail("membre@test.com")).thenReturn(Optional.of(utilisateur));
        when(notificationService.getOwnerId(999L)).thenReturn(1L);
        doThrow(new IllegalArgumentException("Not found")).when(notificationService).marquerCommeLue(999L);

        ResponseEntity<?> response = controller.marquerCommeLue(999L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── helpers ─────────────────────────────────────────────────

    private NotificationDTO buildDTO(Long id, String titre, String message, boolean lu) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(id);
        dto.setTitre(titre);
        dto.setMessage(message);
        dto.setType("general");
        dto.setLu(lu);
        dto.setDate(LocalDateTime.now());
        dto.setUtilisateurId(1L);
        return dto;
    }
}
