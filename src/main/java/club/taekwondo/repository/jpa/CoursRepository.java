package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Cours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoursRepository extends JpaRepository<Cours, Long> {
    // Ajoute des méthodes spécifiques si nécessaire, par exemple pour filtrer par date, nom, etc.
}