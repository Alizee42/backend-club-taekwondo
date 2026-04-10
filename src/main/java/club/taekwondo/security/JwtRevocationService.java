package club.taekwondo.security;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtRevocationService {

    private final JwtUtil jwtUtil;
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public JwtRevocationService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public void revokeToken(String token) {
        cleanupExpiredEntries();
        revokedTokens.put(buildTokenKey(token), jwtUtil.extractExpiration(token).toInstant());
    }

    public boolean isRevoked(String token) {
        cleanupExpiredEntries();
        Instant expiration = revokedTokens.get(buildTokenKey(token));
        return expiration != null && expiration.isAfter(Instant.now());
    }

    void cleanupExpiredEntries() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
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
