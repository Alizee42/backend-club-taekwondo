package club.taekwondo.security;

import club.taekwondo.entity.jpa.RevokedToken;
import club.taekwondo.repository.jpa.RevokedTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtRevocationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private JwtRevocationService service;

    @BeforeEach
    void setUp() {
        service = new JwtRevocationService(jwtUtil, revokedTokenRepository);
    }

    @Test
    void revokeToken_avecJti_utiliseLeJtiCommeCle() {
        when(jwtUtil.extractTokenId("token1")).thenReturn("jti-123");
        when(revokedTokenRepository.existsByTokenKey("jti:jti-123")).thenReturn(false);
        when(jwtUtil.extractExpiration("token1")).thenReturn(new Date(System.currentTimeMillis() + 100000));

        service.revokeToken("token1");

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        assertEquals("jti:jti-123", captor.getValue().getTokenKey());
    }

    @Test
    void revokeToken_dejaRevoque_neSauvegardePas() {
        when(jwtUtil.extractTokenId("token1")).thenReturn("jti-123");
        when(revokedTokenRepository.existsByTokenKey("jti:jti-123")).thenReturn(true);

        service.revokeToken("token1");

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void revokeToken_sansJti_utiliseHashSha256CommeCle() {
        when(jwtUtil.extractTokenId("token-sans-jti")).thenReturn(null);
        when(revokedTokenRepository.existsByTokenKey(anyString())).thenReturn(false);
        when(jwtUtil.extractExpiration("token-sans-jti")).thenReturn(new Date(System.currentTimeMillis() + 100000));

        service.revokeToken("token-sans-jti");

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        assertTrue(captor.getValue().getTokenKey().startsWith("sha256:"));
    }

    @Test
    void revokeToken_extractTokenIdLeveException_bascculeSurHash() {
        when(jwtUtil.extractTokenId("token-invalide")).thenThrow(new RuntimeException("parse error"));
        when(revokedTokenRepository.existsByTokenKey(anyString())).thenReturn(false);
        when(jwtUtil.extractExpiration("token-invalide")).thenReturn(new Date(System.currentTimeMillis() + 100000));

        service.revokeToken("token-invalide");

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        assertTrue(captor.getValue().getTokenKey().startsWith("sha256:"));
    }

    @Test
    void isRevoked_present_retourneTrue() {
        when(jwtUtil.extractTokenId("token1")).thenReturn("jti-123");
        when(revokedTokenRepository.existsByTokenKey("jti:jti-123")).thenReturn(true);

        assertTrue(service.isRevoked("token1"));
    }

    @Test
    void isRevoked_absent_retourneFalse() {
        when(jwtUtil.extractTokenId("token1")).thenReturn("jti-123");
        when(revokedTokenRepository.existsByTokenKey("jti:jti-123")).thenReturn(false);

        assertFalse(service.isRevoked("token1"));
    }

    @Test
    void cleanupExpiredTokens_delegueAuRepository() {
        service.cleanupExpiredTokens();

        verify(revokedTokenRepository).deleteAllExpiredBefore(any(Instant.class));
    }
}
