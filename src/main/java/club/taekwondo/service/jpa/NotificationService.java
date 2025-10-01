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

    // 🔹 Créer et envoyer une notification à un utilisateur (simple)
    public NotificationDTO envoyerNotification(Long utilisateurId, String message) {
        return envoyerNotification(utilisateurId, "Notification", message, "general");
    }
    
    // 🔹 Créer et envoyer une notification complète
    public NotificationDTO envoyerNotification(Long utilisateurId, String titre, String message, String type) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'id : " + utilisateurId));

        Notification notification = new Notification();
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setUtilisateur(utilisateur);
        notification.setLu(false);
        notification.setDateEnvoi(LocalDateTime.now());

        return toDTO(notificationRepository.save(notification));
    }

    // 🔹 Récupérer TOUTES les notifications d'un utilisateur (lues et non lues)
    public List<NotificationDTO> getToutesNotificationsUtilisateur(Long utilisateurId) {
        return notificationRepository.findByUtilisateurIdOrderByDateEnvoiDesc(utilisateurId)
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
    
    // 🔹 Marquer toutes les notifications d'un utilisateur comme lues
    public void marquerToutesCommeLues(Long utilisateurId) {
        List<Notification> notifications = notificationRepository.findByUtilisateurIdAndLuFalse(utilisateurId);
        notifications.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(notifications);
    }

    // 🔹 Supprimer une notification
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification non trouvée avec l'id : " + notificationId));
        notificationRepository.delete(notification);
    }
    
    // 🔹 Envoyer une notification à tous les utilisateurs
    public void envoyerNotificationATous(String titre, String message, String type) {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        for (Utilisateur utilisateur : utilisateurs) {
            envoyerNotification(utilisateur.getId(), titre, message, type);
        }
    }

    // 🔹 Convertir une entité en DTO
    public NotificationDTO toDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setTitre(notification.getTitre());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setLu(notification.isLu());
        dto.setDate(notification.getDateEnvoi());
        dto.setUtilisateurId(notification.getUtilisateur().getId());
        return dto;
    }
}