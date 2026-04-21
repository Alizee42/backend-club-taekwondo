package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Document;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // ---- Filtres simples -----------------------------------------------------

    // Par statut (ex: "validé" / "en attente" / "refusé")
    List<Document> findByStatus(String status);

    // Par utilisateur (Document.utilisateur.id)
    List<Document> findByUtilisateurId(Long utilisateurId);

    // ✅ Nouveau : par membre (Document.membre.id)
    List<Document> findByMembreId(Long membreId);

    /** Ramène tous les documents avec utilisateur + membre (enfant) chargés */
    @EntityGraph(attributePaths = {"utilisateur", "membre"}) // <-- adapte si le champ s'appelle autrement
    List<Document> findAllBy();

    // ---- Versions avec FETCH JOIN (évite N+1 et LazyInitialization) ----------

    // Tous les documents, avec utilisateur ET membre préchargés
    @Query("""
           SELECT d
           FROM Document d
           LEFT JOIN FETCH d.utilisateur u
           LEFT JOIN FETCH u.club
           LEFT JOIN FETCH d.membre m
           LEFT JOIN FETCH m.parent
           """)
    List<Document> findAllWithUtilisateurAndMembre();

    // Par utilisateur, avec utilisateur ET membre préchargés
    @Query("""
           SELECT d
           FROM Document d
           LEFT JOIN FETCH d.utilisateur u
           LEFT JOIN FETCH u.club
           LEFT JOIN FETCH d.membre m
           LEFT JOIN FETCH m.parent
           WHERE u.id = :utilisateurId
           """)
    List<Document> findByUtilisateurIdWithFetch(Long utilisateurId);

    // ✅ Nouveau : par membre, avec utilisateur ET membre préchargés
    @Query("""
           SELECT d
           FROM Document d
           LEFT JOIN FETCH d.utilisateur u
           LEFT JOIN FETCH u.club
           LEFT JOIN FETCH d.membre m
           LEFT JOIN FETCH m.parent
           WHERE m.id = :membreId
           """)
    List<Document> findByMembreIdWithFetch(Long membreId);

    @Query("""
           SELECT d
           FROM Document d
           LEFT JOIN FETCH d.utilisateur u
           LEFT JOIN FETCH u.club
           LEFT JOIN FETCH d.membre m
           LEFT JOIN FETCH m.parent
           WHERE d.id = :id
           """)
    java.util.Optional<Document> findByIdWithFetch(Long id);

    @Query("""
           SELECT d
           FROM Document d
           LEFT JOIN FETCH d.utilisateur u
           LEFT JOIN FETCH u.club
           LEFT JOIN FETCH d.membre m
           LEFT JOIN FETCH m.parent
           WHERE d.status = :status
           """)
    List<Document> findByStatusWithFetch(String status);
}
