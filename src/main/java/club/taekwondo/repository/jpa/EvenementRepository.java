package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {
    // Tu peux ajouter des méthodes spécifiques ici si nécessaire, par exemple pour filtrer par lieu, date, etc.
}
