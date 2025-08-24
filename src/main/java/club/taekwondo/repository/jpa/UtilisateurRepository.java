package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    // Historique (conserve si déjà utilisé quelque part)
    Optional<Utilisateur> findByEmail(String email);

    // Recherches case-insensitive utiles côté service
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<Utilisateur> findByNomIgnoreCaseAndPrenomIgnoreCase(String nom, String prenom);
}

