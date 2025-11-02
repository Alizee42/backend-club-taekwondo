package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.RequiredDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, Long> {
    List<RequiredDocument> findByClub_IdOrderByOrderIndexAsc(Long clubId);
    Optional<RequiredDocument> findByClub_IdAndCode(Long clubId, String code);
    boolean existsByClub_IdAndCode(Long clubId, String code);
}
