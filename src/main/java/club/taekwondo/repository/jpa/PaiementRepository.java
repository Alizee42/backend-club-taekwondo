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

    @EntityGraph(attributePaths = {"echeances", "utilisateur", "membre"})
    @Query("SELECT p FROM Paiement p WHERE p.id = :id")
    Optional<Paiement> findByIdWithDetails(@Param("id") Long id);

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

    // 🔹 Paiements liés à plusieurs membres — ⚠️ avec fetch des échéances
    @EntityGraph(attributePaths = "echeances")
    List<Paiement> findByMembreIdIn(List<Long> membresIds);

    // 🔹 Somme totale filtrée par statut (insensible à la casse)
    @Query("""
        SELECT COALESCE(SUM(p.montantTotal), 0)
        FROM Paiement p
        WHERE LOWER(p.statut) = LOWER(:statut)
    """)
    Double sumMontantByStatut(@Param("statut") String statut);

    /* =========================================================
       ✅ AJOUTS pour reçu Stripe / sync PaymentIntent & Charge
       ========================================================= */

    // ➕ retrouver un paiement via l'ID du PaymentIntent Stripe (ex: "pi_3P...")
    Optional<Paiement> findByPaymentIntentId(String paymentIntentId);

    // ➕ retrouver un paiement via l'ID de la charge (ex: "ch_3P...")
    Optional<Paiement> findByChargeId(String chargeId);

    // ➕ récupérer la receipt_url en direct (utile pour exposer rapidement l’URL)
    @Query("SELECT p.receiptUrl FROM Paiement p WHERE p.id = :id")
    Optional<String> getReceiptUrl(@Param("id") Long paiementId);

    Optional<Paiement> findTopByCommandeIdOrderByIdDesc(Long commandeId);
    // Filtrage par club via Commande uniquement (hist.)
    @Query("SELECT p FROM Paiement p WHERE p.commande.club.id = :clubId")
    List<Paiement> findByClubId(Long clubId);

    // Filtrage par club couvrant Commande, Membre et Utilisateur
    @Query("""
        SELECT p FROM Paiement p
        LEFT JOIN p.commande c
        LEFT JOIN p.membre m
        LEFT JOIN p.utilisateur u
        WHERE (c IS NOT NULL AND c.club.id = :clubId)
           OR (m IS NOT NULL AND m.club.id = :clubId)
           OR (u IS NOT NULL AND u.club.id = :clubId)
    """)
    List<Paiement> findByClubIdAny(Long clubId);
}
