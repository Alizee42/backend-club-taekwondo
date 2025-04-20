package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Professeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfesseurRepository extends JpaRepository<Professeur, Long> {
    List<Professeur> findBySpecialite(String specialite);
}
