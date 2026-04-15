package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    Optional<RevokedToken> findByTokenKey(String tokenKey);

    boolean existsByTokenKey(String tokenKey);

    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredBefore(Instant now);
}
