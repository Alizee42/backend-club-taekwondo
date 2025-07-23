package club.taekwondo.controller.jpa;

import club.taekwondo.dto.NotificationDTO;
import club.taekwondo.service.jpa.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;


    @Test
    void testGetNotificationsByUtilisateur() throws Exception {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(1L);
        dto.setMessage("Test");
        dto.setDateEnvoi(LocalDateTime.now());
        dto.setLu(false);

        when(notificationService.getNotificationsUtilisateur(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/notifications/utilisateur/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].message").value("Test"));
    }

    @Test
    void testMarquerCommeLue() throws Exception {
        doNothing().when(notificationService).marquerCommeLue(1L);

        mockMvc.perform(put("/api/notifications/1/lue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marquée comme lue."));
    }

    @Test
    void testDeleteNotification() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L);

        mockMvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification supprimée avec succès."));
    }

    @Test
    void testMarquerCommeLue_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Notification non trouvée")).when(notificationService).marquerCommeLue(99L);

        mockMvc.perform(put("/api/notifications/99/lue"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification non trouvée"));
    }
}

