package club.taekwondo.security;

import club.taekwondo.entity.jpa.RevokedToken;
import club.taekwondo.repository.jpa.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtRevocationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private JwtRevocationService jwtRevocationService;

    @Test
    void revokeToken_persisteLeToken() {
        Instant future = Instant.now().plusSeconds(120);
        when(jwtUtil.extractTokenId("token-1")).thenReturn("jti-1");
        when(jwtUtil.extractExpiration("token-1")).thenReturn(Date.from(future));
        when(revokedTokenRepository.existsByTokenKey("jti:jti-1")).thenReturn(false);

        jwtRevocationService.revokeToken("token-1");

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void revokeToken_idempotent_siDejaRevoque() {
        when(jwtUtil.extractTokenId("token-2")).thenReturn("jti-2");
        when(revokedTokenRepository.existsByTokenKey("jti:jti-2")).thenReturn(true);

        jwtRevocationService.revokeToken("token-2");

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void isRevoked_retourneTrue_siTokenPresent() {
        when(jwtUtil.extractTokenId("token-3")).thenReturn("jti-3");
        when(revokedTokenRepository.existsByTokenKey("jti:jti-3")).thenReturn(true);

        assertThat(jwtRevocationService.isRevoked("token-3")).isTrue();
    }

    @Test
    void cleanupExpiredTokens_delegueAuRepository() {
        jwtRevocationService.cleanupExpiredTokens();

        verify(revokedTokenRepository).deleteAllExpiredBefore(any(Instant.class));
    }
}
