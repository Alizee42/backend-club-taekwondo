package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Avis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvisRepository extends JpaRepository<Avis, Integer> {
    // Ici, vous pouvez ajouter des méthodes spécifiques si nécessaire
}