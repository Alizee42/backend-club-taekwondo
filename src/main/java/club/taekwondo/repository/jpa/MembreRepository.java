package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {
    Optional<Membre> findByEmail(String email);
}
