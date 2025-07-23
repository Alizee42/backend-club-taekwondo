package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.ReinitialisationMotDePasse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReinitialisationMotDePasseRepository extends JpaRepository<ReinitialisationMotDePasse, Long> {

    Optional<ReinitialisationMotDePasse> findByToken(String token);

    Optional<ReinitialisationMotDePasse> findByUtilisateurIdAndUtiliseFalse(Long utilisateurId);
}
