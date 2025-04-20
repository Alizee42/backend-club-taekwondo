package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.InscriptionEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InscriptionEvenementRepository extends JpaRepository<InscriptionEvenement, Long> {
    // Ajouter des méthodes personnalisées ici si nécessaire, par exemple pour filtrer par statut, membre, etc.
}
