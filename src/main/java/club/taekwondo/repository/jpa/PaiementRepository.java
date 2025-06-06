package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByUtilisateurId(Long utilisateurId); // Paiements d'un membre donné
}