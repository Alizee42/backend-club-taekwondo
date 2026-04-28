package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.enums.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface InscriptionEvenementRepository extends JpaRepository<InscriptionEvenement, Long> {

    // 🔹 Nombre total d'inscriptions pour un événement
    long countByEvenementId(Long evenementId);

    // 🔹 Compter seulement les inscriptions actives (non annulées)
    long countByEvenementIdAndStatutNot(Long evenementId, StatutInscription statut);

    @Query("SELECT COUNT(i) FROM InscriptionEvenement i WHERE i.evenement.id = :evenementId AND i.statut IN ('EN_ATTENTE', 'VALIDEE')")
    long countActiveByEvenementId(@Param("evenementId") Long evenementId);

    // 🔹 Vérifier si un membre est déjà inscrit à un événement
    boolean existsByEvenementIdAndMembreId(Long evenementId, Long membreId);

    // 🔹 Vérifier si un membre est déjà inscrit sauf si statut = ANNULEE
    boolean existsByMembreIdAndEvenementIdAndStatutNot(Long membreId, Long evenementId, StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions d'un événement AVEC les données du membre
    @Query("SELECT i FROM InscriptionEvenement i JOIN FETCH i.membre m JOIN FETCH i.evenement LEFT JOIN FETCH m.compteUtilisateur LEFT JOIN FETCH m.parent WHERE i.evenement.id = :evenementId")
    List<InscriptionEvenement> findByEvenementIdWithMembre(@Param("evenementId") Long evenementId);

    // 🔹 Récupérer les inscriptions d'un événement filtrées par statut AVEC les données du membre
    @Query("SELECT i FROM InscriptionEvenement i JOIN FETCH i.membre m JOIN FETCH i.evenement LEFT JOIN FETCH m.compteUtilisateur LEFT JOIN FETCH m.parent WHERE i.evenement.id = :evenementId AND i.statut = :statut")
    List<InscriptionEvenement> findByEvenementIdAndStatutWithMembre(@Param("evenementId") Long evenementId, @Param("statut") StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions d'un événement (méthode originale pour compatibilité)
    List<InscriptionEvenement> findByEvenementId(Long evenementId);

    // 🔹 Récupérer les inscriptions d'un événement filtrées par statut (méthode originale pour compatibilité)
    List<InscriptionEvenement> findByEvenementIdAndStatut(Long evenementId, StatutInscription statut);

    // 🔹 Récupérer toutes les inscriptions d'un membre
    List<InscriptionEvenement> findByMembreId(Long membreId);

    @Query("SELECT i FROM InscriptionEvenement i JOIN FETCH i.membre JOIN FETCH i.evenement WHERE i.membre.id = :membreId AND i.statut != 'ANNULEE'")
    List<InscriptionEvenement> findActiveByMembreIdWithEvenement(@Param("membreId") Long membreId);

    // 🔹 Supprimer une inscription spécifique (rarement utilisé car on fait un soft delete → statut ANNULEE)
    @Transactional
    void deleteByEvenementIdAndMembreId(Long evenementId, Long membreId);
    
    // 🔹 Supprimer TOUTES les inscriptions d'un événement (pour la suppression complète)
    @Transactional
    void deleteByEvenementId(Long evenementId);

    // 🔹 Récupérer toutes les inscriptions des enfants d'un parent AVEC les données du membre et de l'événement
    @Query("SELECT i FROM InscriptionEvenement i " +
           "JOIN FETCH i.membre m " +
           "JOIN FETCH i.evenement e " +
           "WHERE m.parent.id = :parentId AND i.statut != 'ANNULEE'")
    List<InscriptionEvenement> findByParentIdWithMembreAndEvenement(@Param("parentId") Long parentId);

    // 🔹 Récupérer toutes les inscriptions d'un club
    @Query("SELECT i FROM InscriptionEvenement i JOIN FETCH i.membre m WHERE m.club.id = :clubId")
    List<InscriptionEvenement> findByMembre_Club_Id(@Param("clubId") Long clubId);
}
