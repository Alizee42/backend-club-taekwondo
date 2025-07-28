package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {

    Optional<Membre> findByCompteUtilisateur_Email(String email);

    List<Membre> findByCompteUtilisateur_Id(Long utilisateurId); // 🔧 AJOUT ICI
}
