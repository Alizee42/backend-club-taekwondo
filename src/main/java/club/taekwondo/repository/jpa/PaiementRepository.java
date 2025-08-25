package club.taekwondo.repository.jpa;

import club.taekwondo.dto.DaySumDTO;
import club.taekwondo.dto.StatutCountDTO;
import club.taekwondo.entity.jpa.Paiement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    // 🔹 Récupérer les paiements par utilisateur
    List<Paiement> findByUtilisateurId(Long utilisateurId);

    // 🔹 Récupérer tous les paiements avec leurs échéances (évite le N+1)
    @EntityGraph(attributePaths = "echeances")
    @Query("SELECT p FROM Paiement p")
    List<Paiement> findAllWithEcheances();

    // 🔹 Somme par statut (insensible à la casse, évite NULL via COALESCE)
    @Query("""
        SELECT new club.taekwondo.dto.StatutCountDTO(
            LOWER(p.statut),
            COALESCE(SUM(p.montantTotal), 0)
        )
        FROM Paiement p
        GROUP BY LOWER(p.statut)
    """)
    List<StatutCountDTO> sumByStatut();

    // 🔹 Somme journalière (on groupe par la DATE)
    @Query("""
        SELECT new club.taekwondo.dto.DaySumDTO(
            p.datePaiement,
            COALESCE(SUM(p.montantTotal), 0)
        )
        FROM Paiement p
        WHERE p.datePaiement BETWEEN :from AND :to
        GROUP BY p.datePaiement
        ORDER BY p.datePaiement ASC
    """)
    List<DaySumDTO> sumByDay(@Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    // 🔹 Top retards de paiement (par utilisateur)
    @Query("""
        SELECT p.utilisateur.nom, COALESCE(SUM(p.montantRestant), 0)
        FROM Paiement p
        WHERE p.montantRestant IS NOT NULL
          AND p.montantRestant > 0
        GROUP BY p.utilisateur.nom
        ORDER BY COALESCE(SUM(p.montantRestant), 0) DESC
    """)
    List<Object[]> topRetards(Pageable limit);

    // 🔹 Somme totale sur une période
    @Query("""
        SELECT COALESCE(SUM(p.montantTotal), 0)
        FROM Paiement p
        WHERE p.datePaiement BETWEEN :start AND :end
    """)
    Double sumByDatePaiementBetween(@Param("start") LocalDate start,
                                    @Param("end") LocalDate end);

    // 🔹 Somme totale sur une période filtrée par statut (insensible à la casse)
    @Query("""
        SELECT COALESCE(SUM(p.montantTotal), 0)
        FROM Paiement p
        WHERE LOWER(p.statut) = LOWER(:statut)
          AND p.datePaiement BETWEEN :start AND :end
    """)
    Double sumByStatutAndDatePaiementBetween(@Param("statut") String statut,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);

    // 🔹 Rechercher un paiement spécifique (détection doublon)
    @Query("""
        SELECT p
        FROM Paiement p
        WHERE p.utilisateur.id = :utilisateurId
          AND p.montantTotal = :montantTotal
          AND p.modePaiement = :modePaiement
          AND p.statut = :statut
    """)
    Optional<Paiement> findPaiementByUtilisateurAndMontantAndStatut(@Param("utilisateurId") Long utilisateurId,
                                                                    @Param("montantTotal") Double montantTotal,
                                                                    @Param("modePaiement") String modePaiement,
                                                                    @Param("statut") String statut);

    // 🔹 Comptage simple par statut
    long countByStatut(String statut);

    // 🔹 Comptage insensible à la casse
    long countByStatutIgnoreCase(String statut);

    // 🔹 Paiements liés à plusieurs membres — ⚠️ maintenant avec fetch des échéances
    @EntityGraph(attributePaths = "echeances")
    List<Paiement> findByMembreIdIn(List<Long> membresIds);

    // 🔹 Somme totale filtrée par statut (insensible à la casse)
    @Query("""
        SELECT COALESCE(SUM(p.montantTotal), 0)
        FROM Paiement p
        WHERE LOWER(p.statut) = LOWER(:statut)
    """)
    Double sumMontantByStatut(@Param("statut") String statut);
}

