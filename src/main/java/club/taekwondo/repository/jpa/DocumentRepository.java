package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Récupérer les documents par statut
    List<Document> findByStatus(String status);

    // Récupérer tous les documents avec leurs utilisateurs
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.utilisateur u WHERE u IS NOT NULL")
    List<Document> findAllWithUtilisateur();
    
    // Récupérer les documents par utilisateur
    List<Document> findByUtilisateurId(Long utilisateurId);
}