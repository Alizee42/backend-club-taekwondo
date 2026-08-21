package club.taekwondo.config;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.security.JwtRevocationService;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtRevocationService jwtRevocationService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil, jwtRevocationService, utilisateurService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Utilisateur user(String email, Role role) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    // ---- shouldNotFilter ----

    @Test
    void shouldNotFilter_uriPublicPrefix_retourneTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/contact");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_uploadPrefix_retourneTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/uploads/documents/x.pdf");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_stripeWebhook_retourneTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/stripe/webhook");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_heroConfigEnGet_retourneTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hero-config");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_heroConfigEnPost_retourneFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/hero-config");

        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_optionsPreflight_retourneTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/paiements");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_routeProtegee_retourneFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/paiements/club/1");

        assertFalse(filter.shouldNotFilter(request));
    }

    // ---- doFilterInternal ----

    @Test
    void doFilterInternal_sansHeaderAuthorization_continueEnAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_headerSansBearer_continueEnAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_tokenLitteralNull_continueEnAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer null");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_tokenLitteralUndefined_continueEnAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer undefined");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenRevoque_retourne401EtBloqueLaChaine() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("revoked-token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token revoked"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_emailManquantDansJwt_continueEnAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_authentificationDejaPresente_skip() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@test.com");

        Authentication existing = new UsernamePasswordAuthenticationToken("dejaAuth", null);
        SecurityContextHolder.getContext().setAuthentication(existing);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(utilisateurService, never()).getUtilisateurEntityByEmail(any());
    }

    @Test
    void doFilterInternal_tokenInvalideOuExpire_continueEnAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@test.com");
        when(jwtUtil.validateToken("valid-token", "user@test.com")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_utilisateurIntrouvable_continueEnAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@test.com");
        when(jwtUtil.validateToken("valid-token", "user@test.com")).thenReturn(true);
        when(utilisateurService.getUtilisateurEntityByEmail("user@test.com")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_succes_etablitAuthenticationAvecRoleDeLaBdd() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("admin@test.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("ADMIN");
        when(jwtUtil.validateToken("valid-token", "admin@test.com")).thenReturn(true);
        when(utilisateurService.getUtilisateurEntityByEmail("admin@test.com"))
                .thenReturn(Optional.of(user("admin@test.com", Role.ADMIN)));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("admin@test.com", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void doFilterInternal_roleUtilisateurNull_defautParent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@test.com");
        when(jwtUtil.validateToken("valid-token", "user@test.com")).thenReturn(true);
        when(utilisateurService.getUtilisateurEntityByEmail("user@test.com"))
                .thenReturn(Optional.of(user("user@test.com", null)));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PARENT")));
    }

    @Test
    void doFilterInternal_roleJwtDifferentDuRoleBdd_privilegieLeRoleBdd() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("user@test.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("MEMBRE");
        when(jwtUtil.validateToken("valid-token", "user@test.com")).thenReturn(true);
        when(utilisateurService.getUtilisateurEntityByEmail("user@test.com"))
                .thenReturn(Optional.of(user("user@test.com", Role.ADMIN)));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void doFilterInternal_exceptionInattendue_nettoieLeContexteEtContinue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtRevocationService.isRevoked("valid-token")).thenThrow(new RuntimeException("boom"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
