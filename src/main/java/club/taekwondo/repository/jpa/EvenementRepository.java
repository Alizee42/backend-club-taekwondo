package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    // 🔹 Nombre total d'événements
    long count();

    // 🔹 Compter les événements à venir (date de début >= maintenant)
    long countByDateDebutAfter(LocalDateTime date);

    // 🔹 Récupérer tous les événements d'un club
    List<Evenement> findByClub_Id(Long clubId);
}
