package club.taekwondo.config;

import club.taekwondo.security.JwtRevocationService;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    private final TestableJwtAuthFilter filter = new TestableJwtAuthFilter(
            mock(JwtUtil.class),
            mock(JwtRevocationService.class),
            mock(UtilisateurService.class)
    );

    @Test
    void shouldNotFilter_publicStripeKeyEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/stripe/public-key");

        assertTrue(filter.exposedShouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_createPaymentIntentRequiresJwtProcessing() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/stripe/create-payment-intent");

        assertFalse(filter.exposedShouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_optionsPreflightIsIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/stripe/create-payment-intent");

        assertTrue(filter.exposedShouldNotFilter(request));
    }

    private static final class TestableJwtAuthFilter extends JwtAuthFilter {
        private TestableJwtAuthFilter(JwtUtil jwtUtil,
                                      JwtRevocationService jwtRevocationService,
                                      UtilisateurService utilisateurService) {
            super(jwtUtil, jwtRevocationService, utilisateurService);
        }

        private boolean exposedShouldNotFilter(MockHttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
