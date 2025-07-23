package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Notification;
import club.taekwondo.entity.jpa.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUtilisateur(Utilisateur utilisateur);

    List<Notification> findByUtilisateurIdAndLuFalse(Long utilisateurId);
}
