
package club.taekwondo.repository.jpa;


import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.entity.jpa.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    List<Utilisateur> findByClub_Id(Long clubId);
    List<Utilisateur> findByClub(Club club);
    // Historique (conserve si déjà utilisé quelque part)
    Optional<Utilisateur> findByEmail(String email);

    // Recherches case-insensitive utiles côté service
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<Utilisateur> findByNomIgnoreCaseAndPrenomIgnoreCase(String nom, String prenom);
}

