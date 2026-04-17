package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Galerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalerieRepository extends JpaRepository<Galerie, Long> {
    List<Galerie> findByClubIdOrderByDatePublicationDesc(Long clubId);
    List<Galerie> findAllByOrderByDatePublicationDesc();
}
