package club.taekwondo.config;

import club.taekwondo.entity.jpa.Utilisateur;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UtilisateurService utilisateurService;

    public JwtAuthFilter(JwtUtil jwtUtil, UtilisateurService utilisateurService) {
        this.jwtUtil = jwtUtil;
        this.utilisateurService = utilisateurService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Trace de la requête
        log.debug("[SEC] {} {}", request.getMethod(), request.getRequestURI());

        // Laisse passer les préflights CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // Pas de bearer → anonyme
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("[SEC] Pas de Bearer → anonyme");
            filterChain.doFilter(request, response);
            return;
        }

        // Récupération du token sans le préfixe
        String token = authHeader.substring(7).trim();

        // Cas fréquents en front : "null" / "undefined"
        if (!StringUtils.hasText(token) || "null".equalsIgnoreCase(token) || "undefined".equalsIgnoreCase(token)) {
            log.debug("[SEC] Bearer vide/'null'/'undefined' → anonyme");
            filterChain.doFilter(request, response);
            return;
        }

        String tail = token.length() > 8 ? token.substring(token.length() - 8) : token;
        log.debug("[SEC] Bearer présent (len={}, tail=...{})", token.length(), tail);

        try {
            String email = jwtUtil.extractEmail(token);
            log.debug("[SEC] email extrait du JWT = {}", email);

            // Si pas d'email ou auth déjà positionnée → on continue
            if (!StringUtils.hasText(email)) {
                log.warn("[SEC] Email manquant dans le JWT");
                filterChain.doFilter(request, response);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("[SEC] Auth déjà présente dans le contexte → skip setAuthentication");
                filterChain.doFilter(request, response);
                return;
            }

            // Valider le token d'abord (signature/expiration/sujet)
            if (!jwtUtil.validateToken(token, email)) {
                log.warn("[SEC] JWT invalide/expiré pour {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            // Charger l'utilisateur
            Optional<Utilisateur> opt = utilisateurService.getUtilisateurEntityByEmail(email);
            if (opt.isEmpty()) {
                log.warn("[SEC] Utilisateur introuvable pour {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            Utilisateur user = opt.get();

            // Construire les authorities depuis le rôle (enum)
            String role = (user.getRole() != null) ? user.getRole().name().toUpperCase() : "PARENT";
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(role));            // ex: "ADMIN"
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));  // ex: "ROLE_ADMIN"

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("[SEC] Auth OK: user={} roles={}", email, authorities);

        } catch (Exception ex) {
            // En cas d'erreur, on nettoie le contexte et on laisse passer → @PreAuthorize tranchera.
            SecurityContextHolder.clearContext();
            log.warn("[SEC] Erreur parsing/validation JWT: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

