package club.taekwondo.security;

import club.taekwondo.entity.jpa.RevokedToken;
import club.taekwondo.repository.jpa.RevokedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class JwtRevocationService {

    private final JwtUtil jwtUtil;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtRevocationService(JwtUtil jwtUtil, RevokedTokenRepository revokedTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public void revokeToken(String token) {
        String key = buildTokenKey(token);
        if (!revokedTokenRepository.existsByTokenKey(key)) {
            Instant expiresAt = jwtUtil.extractExpiration(token).toInstant();
            revokedTokenRepository.save(new RevokedToken(key, expiresAt));
        }
    }

    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByTokenKey(buildTokenKey(token));
    }

    /** Nettoyage quotidien des tokens expirés pour ne pas surcharger la table. */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        revokedTokenRepository.deleteAllExpiredBefore(Instant.now());
    }

    private String buildTokenKey(String token) {
        String tokenId = null;
        try {
            tokenId = jwtUtil.extractTokenId(token);
        } catch (Exception ignored) {
        }
        if (StringUtils.hasText(tokenId)) {
            return "jti:" + tokenId;
        }
        return "sha256:" + sha256(token);
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de calculer l'empreinte du token", e);
        }
    }
}
