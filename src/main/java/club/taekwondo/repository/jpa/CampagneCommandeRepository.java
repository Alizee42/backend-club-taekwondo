package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.CampagneCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CampagneCommandeRepository extends JpaRepository<CampagneCommande, Long> {

    List<CampagneCommande> findByClub_Id(Long clubId);

    @Query("SELECT c FROM CampagneCommande c WHERE c.actif = true AND c.club.id = :clubId AND c.dateOuverture <= :today AND c.dateFermeture >= :today")
    Optional<CampagneCommande> findActivePourClub(@Param("clubId") Long clubId, @Param("today") LocalDate today);
}
