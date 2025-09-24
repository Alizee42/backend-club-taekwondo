package club.taekwondo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24h
    private long jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ====== extraction ======
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUsername(String token) {
        return extractEmail(token);
    }

    public String extractRole(String token) {
        Claims c = extractAllClaims(token);
        Object r = c.get("role");
        return r == null ? null : String.valueOf(r);
    }

    public Object extractUtilisateurId(String token) {
        return extractAllClaims(token).get("utilisateurId");
    }

    public Object extractMembreId(String token) {
        return extractAllClaims(token).get("membreId");
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, String email) {
        String subject = extractEmail(token);
        return subject != null && subject.equals(email) && !isTokenExpired(token);
    }

    // ====== génération ======

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Génère un token simple avec role */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .addClaims(Map.of("role", role))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 🔥 Génère un token complet avec ID + role */
    public String generateToken(String email, String role, Long utilisateurId, Long membreId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (utilisateurId != null) {
            claims.put("utilisateurId", utilisateurId);
        }
        if (membreId != null) {
            claims.put("membreId", membreId);
        }

        return Jwts.builder()
        .setSubject(email) // "sub"
        .addClaims(claims) // ajoute role, utilisateurId, membreId
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();

    }
}
