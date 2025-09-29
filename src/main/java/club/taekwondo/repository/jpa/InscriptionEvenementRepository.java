package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.enums.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscriptionEvenementRepository extends JpaRepository<InscriptionEvenement, Long> {

    // 🔹 Nombre total d’inscriptions pour un événement
    long countByEvenementId(Long evenementId);

    // 🔹 Compter seulement les inscriptions actives (non annulées)
    long countByEvenementIdAndStatutNot(Long evenementId, StatutInscription statut);

    // 🔹 Vérifier si un membre est déjà inscrit à un événement
    boolean existsByEvenementIdAndMembreId(Long evenementId, Long membreId);

    // 🔹 Vérifier si un membre est déjà inscrit sauf si statut = ANNULEE
    boolean existsByMembreIdAndEvenementIdAndStatutNot(Long membreId, Long evenementId, StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions d’un événement
    List<InscriptionEvenement> findByEvenementId(Long evenementId);

    // 🔹 Récupérer les inscriptions d’un événement filtrées par statut
    List<InscriptionEvenement> findByEvenementIdAndStatut(Long evenementId, StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions d’un membre
    List<InscriptionEvenement> findByMembreId(Long membreId);

    // 🔹 Supprimer une inscription spécifique (rarement utilisé car on fait un soft delete → statut ANNULEE)
    void deleteByEvenementIdAndMembreId(Long evenementId, Long membreId);
}
