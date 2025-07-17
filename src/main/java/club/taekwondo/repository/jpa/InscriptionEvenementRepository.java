package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.enums.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscriptionEvenementRepository extends JpaRepository<InscriptionEvenement, Long> {

    // 🔹 Vérifier si un utilisateur est déjà inscrit à un événement (sauf si le statut est ANNULEE)
    boolean existsByUtilisateurIdAndEvenementIdAndStatutNot(Long utilisateurId, Long evenementId, StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions pour un événement donné
    List<InscriptionEvenement> findByEvenementId(Long evenementId);

    // 🔹 Récupérer toutes les inscriptions pour un événement donné avec un statut spécifique
    List<InscriptionEvenement> findByEvenementIdAndStatut(Long evenementId, StatutInscription statut);
}