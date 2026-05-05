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
        doNothing().when(notificationService).marquerCommeLue(5L);

        ResponseEntity<?> response = controller.markAsRead(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).marquerCommeLue(5L);
    }

    @Test
    void markAsRead_notificationInexistante_retourne404() {
        doThrow(new IllegalArgumentException("Not found")).when(notificationService).marquerCommeLue(999L);

        ResponseEntity<?> response = controller.markAsRead(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
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
        doNothing().when(notificationService).deleteNotification(3L);

        ResponseEntity<?> response = controller.deleteNotification(3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).deleteNotification(3L);
    }

    @Test
    void deleteNotification_inexistante_retourne404() {
        doThrow(new IllegalArgumentException("Not found")).when(notificationService).deleteNotification(888L);

        ResponseEntity<?> response = controller.deleteNotification(888L);

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
