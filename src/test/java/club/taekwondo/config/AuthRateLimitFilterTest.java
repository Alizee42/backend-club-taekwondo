package club.taekwondo.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter();

    @Test
    void shouldLimitRepeatedLoginAttempts() throws ServletException, IOException {
        for (int attempt = 1; attempt <= 5; attempt++) {
            MockHttpServletResponse response = execute("/api/utilisateurs/login");
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        MockHttpServletResponse blockedResponse = execute("/api/utilisateurs/login");
        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getContentAsString()).contains("Trop de tentatives d'authentification");
    }

    @Test
    void shouldIgnoreNonAuthEndpoints() throws ServletException, IOException {
        MockHttpServletResponse response = execute("/api/utilisateurs/me");
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    private MockHttpServletResponse execute(String path) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.setServletPath(path);
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
