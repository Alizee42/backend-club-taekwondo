package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Commande;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // 🔹 Trouver les commandes par statut et mode de paiement
    List<Commande> findByStatutAndModePaiement(String statut, String modePaiement);

    // 🔹 Trouver les commandes à payer au club (espèces ou virement)
    @Query("SELECT c FROM Commande c WHERE c.statut = :statut AND c.modePaiement IN (:modesPaiement)")
    List<Commande> findCommandesPaiementClub(
            @Param("statut") String statut,
            @Param("modesPaiement") List<String> modesPaiement);

    // 🔹 Trouver les commandes par utilisateur
    List<Commande> findByUtilisateurId(Long utilisateurId);
}