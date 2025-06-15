package club.taekwondo.repository.jpa;

import club.taekwondo.dto.DaySumDTO;
import club.taekwondo.dto.StatutCountDTO;
import club.taekwondo.entity.jpa.Paiement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    // 🔥 Requête : trouver les paiements par utilisateur
    List<Paiement> findByUtilisateurId(Long utilisateurId);

    // 🔥 Requête : tous les paiements avec leurs échéances
    @Query("SELECT DISTINCT p FROM Paiement p LEFT JOIN FETCH p.echeances")
    List<Paiement> findAllWithEcheances();

    // 🔥 Requête corrigée : somme des montants par statut
    @Query("""
        SELECT new club.taekwondo.dto.StatutCountDTO(p.statut, SUM(p.montantTotal))
        FROM Paiement p
        GROUP BY p.statut
    """)
    List<StatutCountDTO> sumByStatut();

    // 🔥 Requête corrigée : somme des montants par jour (-30 jours)
    @Query("""
        SELECT new club.taekwondo.dto.DaySumDTO(p.datePaiement, SUM(p.montantTotal))
        FROM Paiement p
        WHERE p.datePaiement >= :from
        GROUP BY p.datePaiement
        ORDER BY p.datePaiement ASC
    """)
    List<DaySumDTO> sumByDay(@Param("from") LocalDate from);

    // 🔥 Requête corrigée : top 5 utilisateurs avec retards
    @Query("""
        SELECT p.utilisateur.nom, SUM(p.montantRestant)
        FROM Paiement p
        WHERE p.montantRestant > 0
        GROUP BY p.utilisateur.nom
        ORDER BY SUM(p.montantRestant) DESC
    """)
    List<Object[]> topRetards(Pageable limit);

    // 🔥 Requête corrigée : somme des montants payés entre deux dates
    @Query("""
        SELECT COALESCE(SUM(p.montantTotal), 0)
        FROM Paiement p
        WHERE p.datePaiement BETWEEN :start AND :end
    """)
    Long sumByDatePaiementBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // 🔥 Requête corrigée : somme des montants payés par statut entre deux dates
    @Query("""
        SELECT COALESCE(SUM(p.montantTotal), 0)
        FROM Paiement p
        WHERE p.statut = :statut AND p.datePaiement BETWEEN :start AND :end
    """)
    Long sumByStatutAndDatePaiementBetween(@Param("statut") String statut, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // 🔥 Requête : trouver un paiement existant (doublon)
    @Query("""
        SELECT p
        FROM Paiement p
        WHERE p.utilisateur.id = :utilisateurId
          AND p.montantTotal = :montantTotal
          AND p.modePaiement = :modePaiement
          AND p.statut = :statut
    """)
    Optional<Paiement> findPaiementByUtilisateurAndMontantAndStatut(
        @Param("utilisateurId") Long utilisateurId,
        @Param("montantTotal") Double montantTotal,
        @Param("modePaiement") String modePaiement,
        @Param("statut") String statut
    );
}