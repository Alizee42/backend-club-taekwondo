package club.taekwondo.repository.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import club.taekwondo.entity.jpa.Echeance;

@Repository
public interface EcheanceRepository extends JpaRepository<Echeance, Long> {

    /* =======================
       Chargements avec relations
       ======================= */

    // findAll avec EntityGraph pour charger paiement + utilisateur (parent) + membre (enfant)
    @Override
    @EntityGraph(attributePaths = {"paiement", "paiement.utilisateur", "paiement.membre"})
    List<Echeance> findAll();

    // Récupération des échéances d’un paiement, triées par numéro, avec relations chargées
    @EntityGraph(attributePaths = {"paiement", "paiement.utilisateur", "paiement.membre"})
    List<Echeance> findByPaiementIdOrderByNumeroAsc(Long paiementId);

    // Échéances en retard, avec relations chargées (utile pour getMembresEnRetard)
    @EntityGraph(attributePaths = {"paiement", "paiement.utilisateur", "paiement.membre"})
    List<Echeance> findByStatutAndDateEcheanceBefore(String statut, LocalDate date);

    /* -----------------------------------------------
       (Alternative si vous préférez une méthode dédiée)
       // @Query("""
       //   select e from Echeance e
       //   join fetch e.paiement p
       //   left join fetch p.utilisateur
       //   left join fetch p.membre
       // """)
       // List<Echeance> findAllWithJoins();
       ----------------------------------------------- */
}
