package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Ajouter des méthodes spécifiques si nécessaire, par exemple pour filtrer par type, membre, etc.
}
