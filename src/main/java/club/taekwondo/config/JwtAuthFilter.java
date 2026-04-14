package club.taekwondo.config;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtRevocationService;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final JwtRevocationService jwtRevocationService;
    private final UtilisateurService utilisateurService;

    // Routes publiques (aucun traitement JWT)
    private static final List<String> PUBLIC_PREFIXES = List.of(
        "/error",
        "/api/stripe/public-key",
        "/api/stripe/webhook",
        "/api/stripe/create-payment-intent",
        "/api/utilisateurs/login",
        "/api/utilisateurs/register",
        "/api/parametres-paiement/public",
        "/api/public",
        "/api/uploads" // Ajouté : accès libre aux fichiers/documents
    );

    public JwtAuthFilter(JwtUtil jwtUtil,
                         JwtRevocationService jwtRevocationService,
                         UtilisateurService utilisateurService) {
        this.jwtUtil = jwtUtil;
        this.jwtRevocationService = jwtRevocationService;
        this.utilisateurService = utilisateurService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return false;

        // ⚡ Ignore les endpoints publics
        boolean isPublic = PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith);
        if (isPublic) {
            log.debug("[SEC] Endpoint public ignoré: {}", uri);
            return true;
        }

        // ⚡ Ignore les préflights CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("[SEC] Preflight CORS ignoré");
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("[SEC] {} {}", request.getMethod(), request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        log.debug("[SEC] Header Authorization: {}", authHeader);

        // Pas de Bearer → anonyme
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("[SEC] Pas de Bearer → anonyme");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        // Cas fréquents : "null" / "undefined"
        if (!StringUtils.hasText(token)
                || "null".equalsIgnoreCase(token)
                || "undefined".equalsIgnoreCase(token)) {
            log.debug("[SEC] Bearer vide/'null'/'undefined' → anonyme");
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("[SEC] Bearer présent (len={})", token.length());

        try {
            if (jwtRevocationService.isRevoked(token)) {
                log.warn("[SEC] JWT revoque pour {}", request.getRequestURI());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Token revoked\"}");
                return;
            }

            String email = jwtUtil.extractEmail(token);
            String roleFromToken = jwtUtil.extractRole(token);

            log.debug("[SEC] email extrait du JWT = {}", email);
            log.debug("[SEC] rôle extrait du JWT = {}", roleFromToken);

            if (!StringUtils.hasText(email)) {
                log.warn("[SEC] Email manquant dans le JWT");
                filterChain.doFilter(request, response);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("[SEC] Auth déjà présente → skip setAuthentication");
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtUtil.validateToken(token, email)) {
                log.warn("[SEC] JWT invalide/expiré pour {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            Optional<Utilisateur> opt = utilisateurService.getUtilisateurEntityByEmail(email);
            if (opt.isEmpty()) {
                log.warn("[SEC] Utilisateur introuvable pour {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            Utilisateur user = opt.get();
            log.debug("[SEC] Utilisateur trouvé: {} {}", user.getPrenom(), user.getNom());

            // --- Normalisation du rôle depuis la BDD ---
            String rawRole = (user.getRole() == null)
                    ? "PARENT"
                    : user.getRole().toString().trim().toUpperCase();

            if (rawRole.startsWith("ROLE_")) {
                rawRole = rawRole.substring(5);
            }
            String roleFromDb = rawRole.isBlank() ? "PARENT" : rawRole;

            log.debug("[SEC] rôle BDD normalisé = {}", roleFromDb);

            // 🚨 Comparaison JWT vs BDD
            if (roleFromToken != null && !roleFromToken.equalsIgnoreCase(roleFromDb)) {
                log.warn("[SEC] ⚠ Rôle JWT ({}) ≠ rôle BDD ({})", roleFromToken, roleFromDb);
            }

            // ✅ On ne garde que ROLE_xxx
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + roleFromDb));

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("[SEC] Auth OK: user={} authorities={}", email, authorities);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("[SEC] Erreur parsing/validation JWT: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
