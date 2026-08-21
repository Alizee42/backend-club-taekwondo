package club.taekwondo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "0123456789012345678901234567890123456789");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 86400000L);
    }

    @Test
    void generateToken_simple_extraitEmail() {
        String token = jwtUtil.generateToken("user@test.com");

        assertEquals("user@test.com", jwtUtil.extractEmail(token));
        assertEquals("user@test.com", jwtUtil.extractUsername(token));
    }

    @Test
    void generateToken_avecRole_extraitRole() {
        String token = jwtUtil.generateToken("user@test.com", "ADMIN");

        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void generateToken_sansRole_extractRoleRetourneNull() {
        String token = jwtUtil.generateToken("user@test.com");

        assertNull(jwtUtil.extractRole(token));
    }

    @Test
    void generateToken_complet_extraitTousLesClaims() {
        String token = jwtUtil.generateToken("user@test.com", "MEMBRE", 1L, 8L);

        assertEquals("user@test.com", jwtUtil.extractEmail(token));
        assertEquals("MEMBRE", jwtUtil.extractRole(token));
        assertEquals(1, ((Number) jwtUtil.extractUtilisateurId(token)).intValue());
        assertEquals(8, ((Number) jwtUtil.extractMembreId(token)).intValue());
    }

    @Test
    void generateToken_complet_sansMembreId_neContientPasLeClaim() {
        String token = jwtUtil.generateToken("user@test.com", "ADMIN", 1L, null);

        assertNull(jwtUtil.extractMembreId(token));
    }

    @Test
    void generateToken_complet_sansUtilisateurId_neContientPasLeClaim() {
        String token = jwtUtil.generateToken("user@test.com", "ADMIN", null, null);

        assertNull(jwtUtil.extractUtilisateurId(token));
    }

    @Test
    void extractTokenId_estUnique() {
        String token1 = jwtUtil.generateToken("user@test.com");
        String token2 = jwtUtil.generateToken("user@test.com");

        assertNotNull(jwtUtil.extractTokenId(token1));
        assertNotNull(jwtUtil.extractTokenId(token2));
        assertFalse(jwtUtil.extractTokenId(token1).equals(jwtUtil.extractTokenId(token2)));
    }

    @Test
    void extractExpiration_estDansLeFutur() {
        String token = jwtUtil.generateToken("user@test.com");

        assertTrue(jwtUtil.extractExpiration(token).after(new Date()));
    }

    @Test
    void validateToken_emailCorrectEtNonExpire_retourneTrue() {
        String token = jwtUtil.generateToken("user@test.com");

        assertTrue(jwtUtil.validateToken(token, "user@test.com"));
    }

    @Test
    void validateToken_emailIncorrect_retourneFalse() {
        String token = jwtUtil.generateToken("user@test.com");

        assertFalse(jwtUtil.validateToken(token, "autre@test.com"));
    }

    @Test
    void validateToken_expire_leveExpiredJwtException() {
        // Bug connu : la lib JJWT leve ExpiredJwtException des le parsing des claims
        // (extractEmail, premiere ligne de validateToken), avant meme d'atteindre le
        // check isTokenExpired(). validateToken() ne renvoie donc jamais false pour un
        // token expire : il propage l'exception.
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1000L);
        String token = jwtUtil.generateToken("user@test.com");

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtUtil.validateToken(token, "user@test.com"));
    }

    @Test
    void extractClaim_secretAbsent_leveIllegalState() {
        ReflectionTestUtils.setField(jwtUtil, "secret", null);

        assertThrows(IllegalStateException.class, () -> jwtUtil.generateToken("user@test.com"));
    }

    @Test
    void extractClaim_secretTropCourt_leveIllegalState() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "trop-court");

        assertThrows(IllegalStateException.class, () -> jwtUtil.generateToken("user@test.com"));
    }

    @Test
    void extractEmail_tokenInvalide_leveException() {
        assertThrows(Exception.class, () -> jwtUtil.extractEmail("token.invalide.xyz"));
    }

    @Test
    void extractEmail_signeAvecAutreSecret_leveException() {
        String token = jwtUtil.generateToken("user@test.com");

        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secret", "autre-secret-de-32-caracteres-minimum-ok");
        ReflectionTestUtils.setField(otherJwtUtil, "jwtExpirationMs", 86400000L);

        assertThrows(Exception.class, () -> otherJwtUtil.extractEmail(token));
    }
}
