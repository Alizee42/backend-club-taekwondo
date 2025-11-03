package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {
    List<Enseignant> findByClub_Id(Long clubId);
}
