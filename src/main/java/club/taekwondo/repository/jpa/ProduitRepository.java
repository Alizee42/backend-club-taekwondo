package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    List<Produit> findByClub_Id(Long clubId);
    Optional<Produit> findByNom(String nom);
    boolean existsByNom(String nom); // Méthode pour vérifier l'existence par nom
}
