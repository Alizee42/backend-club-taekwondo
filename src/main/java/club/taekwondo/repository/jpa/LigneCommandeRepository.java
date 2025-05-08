package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.LigneCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {
    // Vous pouvez ajouter des méthodes personnalisées ici si nécessaire
}