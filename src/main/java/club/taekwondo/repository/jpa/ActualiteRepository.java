package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Actualite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActualiteRepository extends JpaRepository<Actualite, Long> {
    List<Actualite> findAllByOrderByDatePublicationDesc();
    List<Actualite> findByClubIdOrderByDatePublicationDesc(Long clubId);
    List<Actualite> findByFeaturedTrueOrderByDatePublicationDesc();
    List<Actualite> findByClubIdAndFeaturedTrue(Long clubId);
}
