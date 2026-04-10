package club.taekwondo.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtRevocationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtRevocationService jwtRevocationService;

    @Test
    void shouldMarkRevokedTokenAsRevoked() {
        Instant future = Instant.now().plusSeconds(120);
        when(jwtUtil.extractTokenId("token-1")).thenReturn("jti-1");
        when(jwtUtil.extractExpiration("token-1")).thenReturn(Date.from(future));

        jwtRevocationService.revokeToken("token-1");

        assertThat(jwtRevocationService.isRevoked("token-1")).isTrue();
    }

    @Test
    void shouldDropExpiredRevocationEntries() {
        Instant past = Instant.now().minusSeconds(10);
        when(jwtUtil.extractTokenId("token-2")).thenReturn("jti-2");
        when(jwtUtil.extractExpiration("token-2")).thenReturn(Date.from(past));

        jwtRevocationService.revokeToken("token-2");

        assertThat(jwtRevocationService.isRevoked("token-2")).isFalse();
    }
}
