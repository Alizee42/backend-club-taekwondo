package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.AboutConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AboutConfigRepository extends JpaRepository<AboutConfig, Long> {
    Optional<AboutConfig> findByClub_Id(Long clubId);
}
