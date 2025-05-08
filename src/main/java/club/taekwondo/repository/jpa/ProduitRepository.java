package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    // Vous pouvez ajouter des méthodes personnalisées ici si nécessaire
}