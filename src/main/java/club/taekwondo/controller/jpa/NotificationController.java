package club.taekwondo.controller.jpa;

import club.taekwondo.dto.NotificationDTO;
import club.taekwondo.service.jpa.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 🔹 Envoyer une notification à un utilisateur
    @PostMapping("/envoyer")
    public ResponseEntity<?> envoyerNotification(@RequestParam Long utilisateurId, @RequestParam String message) {
        try {
            NotificationDTO dto = notificationService.envoyerNotification(utilisateurId, message);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 🔹 Récupérer les notifications non lues d'un utilisateur
    @GetMapping("/utilisateur/{id}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsUtilisateur(@PathVariable Long id) {
        List<NotificationDTO> notifications = notificationService.getNotificationsUtilisateur(id);
        return ResponseEntity.ok(notifications);
    }

    // 🔹 Marquer une notification comme lue
    @PutMapping("/{id}/lue")
    public ResponseEntity<?> marquerCommeLue(@PathVariable Long id) {
        try {
            notificationService.marquerCommeLue(id);
            return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    // 🔹 Supprimer une notification
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok(Map.of("message", "Notification supprimée avec succès."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}