package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByClub_Id(Long clubId);

    // 🔹 Trouver les commandes par statut et mode de paiement
    List<Commande> findByStatutAndModePaiement(String statut, String modePaiement);

    // 🔹 Trouver les commandes à payer au club (espèces ou virement)
    @Query("SELECT c FROM Commande c WHERE c.statut = :statut AND c.modePaiement IN (:modesPaiement)")
    List<Commande> findCommandesPaiementClub(
            @Param("statut") String statut,
            @Param("modesPaiement") List<String> modesPaiement);

    // 🔹 Trouver les commandes par utilisateur (parent/membre avec compte)
    List<Commande> findByUtilisateurId(Long utilisateurId);

    // 🔹 Trouver les commandes par bénéficiaire (membre enfant)
    @Query("SELECT DISTINCT c FROM Commande c JOIN c.lignes l WHERE l.beneficiaire.id = :membreId")
    List<Commande> findByMembreId(@Param("membreId") Long membreId);

    // 🔹 Toutes les commandes triées par date (utile pour l’admin)
    List<Commande> findAllByOrderByDateCommandeDesc();
}
