package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.PolitiqueConfidentialiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolitiqueConfidentialiteConfigRepository extends JpaRepository<PolitiqueConfidentialiteConfig, Long> {
    Optional<PolitiqueConfidentialiteConfig> findByClub_Id(Long clubId);
}
