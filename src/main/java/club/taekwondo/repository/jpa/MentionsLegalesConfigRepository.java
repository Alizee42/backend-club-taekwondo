package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.MentionsLegalesConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MentionsLegalesConfigRepository extends JpaRepository<MentionsLegalesConfig, Long> {
    Optional<MentionsLegalesConfig> findByClub_Id(Long clubId);
}
