package club.taekwondo.service.jpa;

import club.taekwondo.dto.NotificationDTO;
import club.taekwondo.entity.jpa.Notification;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 🔹 Créer et envoyer une notification à un utilisateur (simple)
    public NotificationDTO envoyerNotification(Long utilisateurId, String message) {
        return envoyerNotification(utilisateurId, "Notification", message, "general", null);
    }
    
    // 🔹 Créer et envoyer une notification complète (sans lienAction)
    public NotificationDTO envoyerNotification(Long utilisateurId, String titre, String message, String type) {
        return envoyerNotification(utilisateurId, titre, message, type, null);
    }

    // 🔹 Créer et envoyer une notification complète avec lienAction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDTO envoyerNotification(Long utilisateurId, String titre, String message, String type, String lienAction) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'id : " + utilisateurId));

        Notification notification = new Notification();
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLienAction(lienAction);
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

    // 🔹 Propriétaire d'une notification (pour vérifier les droits avant modification/suppression)
    public Long getOwnerId(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification non trouvée avec l'id : " + notificationId));
        return notification.getUtilisateur().getId();
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
            envoyerNotification(utilisateur.getId(), titre, message, type, null);
        }
    }

    // 🔹 Envoyer une notification à tous les admins d'un club
    public void envoyerNotificationAuxAdminsClub(Long clubId, String titre, String message, String type, String lienAction) {
        List<Utilisateur> admins = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() != null
                        && (u.getRole().name().equals("ADMIN"))
                        && u.getClub() != null
                        && Objects.equals(u.getClub().getId(), clubId))
                .collect(java.util.stream.Collectors.toList());
        for (Utilisateur admin : admins) {
            envoyerNotification(admin.getId(), titre, message, type, lienAction);
        }
    }

    // 🔹 Envoyer une notification à tous les super-admins
    public void envoyerNotificationAuxSuperAdmins(String titre, String message, String type, String lienAction) {
        List<Utilisateur> superAdmins = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("SUPER_ADMIN"))
                .collect(java.util.stream.Collectors.toList());
        for (Utilisateur sa : superAdmins) {
            envoyerNotification(sa.getId(), titre, message, type, lienAction);
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
        dto.setLienAction(notification.getLienAction());
        return dto;
    }
}