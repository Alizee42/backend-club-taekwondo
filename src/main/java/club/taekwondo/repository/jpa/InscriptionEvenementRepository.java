package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.enums.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscriptionEvenementRepository extends JpaRepository<InscriptionEvenement, Long> {

    // 🔹 Nombre total d’inscriptions pour un événement (utile pour vérifier la capacité)
    long countByEvenementId(Long evenementId);

    // 🔹 Vérifier si un utilisateur est déjà inscrit à un événement
    boolean existsByEvenementIdAndUtilisateurId(Long evenementId, Long utilisateurId);

    // 🔹 Vérifier si un utilisateur est déjà inscrit sauf si le statut est ANNULEE
    boolean existsByUtilisateurIdAndEvenementIdAndStatutNot(Long utilisateurId, Long evenementId, StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions d’un événement
    List<InscriptionEvenement> findByEvenementId(Long evenementId);

    // 🔹 Récupérer les inscriptions d’un événement filtrées par statut
    List<InscriptionEvenement> findByEvenementIdAndStatut(Long evenementId, StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions d’un utilisateur
    List<InscriptionEvenement> findByUtilisateurId(Long utilisateurId);

    // 🔹 Supprimer une inscription spécifique (rarement utilisé car on fait un soft delete → statut ANNULEE)
    void deleteByEvenementIdAndUtilisateurId(Long evenementId, Long utilisateurId);
}
