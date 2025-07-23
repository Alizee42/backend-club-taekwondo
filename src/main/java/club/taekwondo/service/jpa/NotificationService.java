package club.taekwondo.service.jpa;

import club.taekwondo.dto.NotificationDTO;
import club.taekwondo.entity.jpa.Notification;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 🔹 Créer et envoyer une notification à un utilisateur
    public NotificationDTO envoyerNotification(Long utilisateurId, String message) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'id : " + utilisateurId));

        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setUtilisateur(utilisateur);
        notification.setLu(false);
        notification.setDateEnvoi(LocalDateTime.now());

        return toDTO(notificationRepository.save(notification));
    }

    // 🔹 Récupérer les notifications non lues d’un utilisateur
    public List<NotificationDTO> getNotificationsUtilisateur(Long utilisateurId) {
        return notificationRepository.findByUtilisateurIdAndLuFalse(utilisateurId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Marquer une notification comme lue
    public void marquerCommeLue(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification non trouvée avec l'id : " + notificationId));
        notification.setLu(true);
        notificationRepository.save(notification);
    }

    // 🔹 Supprimer une notification
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification non trouvée avec l'id : " + notificationId));
        notificationRepository.delete(notification);
    }

    // 🔹 Convertir une entité en DTO
    public NotificationDTO toDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setMessage(notification.getMessage());
        dto.setLu(notification.isLu());
        dto.setDateEnvoi(notification.getDateEnvoi());
        dto.setUtilisateurId(notification.getUtilisateur().getId());
        return dto;
    }
}
