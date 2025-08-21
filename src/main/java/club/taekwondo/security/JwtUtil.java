package club.taekwondo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    // secret conseillé : au moins 256 bits (32+ chars) ; idéalement base64
    @Value("${jwt.secret}")
    private String secret;

    // 10h par défaut
    @Value("${jwt.expiration-ms:36000000}")
    private long expirationMs;

    private Key signingKey() {
        // Si ton secret est déjà base64, décode le :
        // byte[] keyBytes = Decoders.BASE64.decode(secret);
        // return Keys.hmacShaKeyFor(keyBytes);

        // Si c’est une chaîne “brute” suffisamment longue :
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Génère un token avec email (subject) + rôle */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .addClaims(Map.of("role", role))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Parse + valide la signature ; lève une exception si invalide/expiré */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object r = extractAllClaims(token).get("role");
        return r != null ? r.toString() : null;
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token) {
        try {
            Date exp = extractExpiration(token);
            return exp != null && exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // signature invalide, token malformé, expiré, etc.
            return false;
        }
    }

    /** Utilitaire : extrait le token d’un header "Authorization: Bearer xxx" */
    public static String resolveFromAuthorizationHeader(String header) {
        if (header == null) return null;
        if (!header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }
}
