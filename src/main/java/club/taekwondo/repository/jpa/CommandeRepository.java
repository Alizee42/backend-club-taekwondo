package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    // Vous pouvez ajouter des méthodes personnalisées ici si nécessaire
}