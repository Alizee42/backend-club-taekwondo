package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    Optional<Produit> findByNom(String nom);
    boolean existsByNom(String nom); // Méthode pour vérifier l'existence par nom
}
