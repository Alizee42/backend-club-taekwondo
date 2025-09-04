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
import java.util.*;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UtilisateurService utilisateurService;

    // Routes publiques (on ne traite pas le JWT ici)
    private static final List<String> PUBLIC_PREFIXES = List.of(
    	    "/error",
    	    "/api/stripe/public-key",
    	    "/api/stripe/webhook",
    	    "/api/stripe/create-payment-intent",  
    	    "/api/utilisateurs/login",
    	    "/api/utilisateurs/register",
    	    "/api/parametres-paiement/public",
    	    "/api/avis",
    	    "/api/actualites",
    	    "/api/debug/"
    	);


    public JwtAuthFilter(JwtUtil jwtUtil, UtilisateurService utilisateurService) {
        this.jwtUtil = jwtUtil;
        this.utilisateurService = utilisateurService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return false;
        for (String p : PUBLIC_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        // Laisse passer les préflights CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("[SEC] {} {}", request.getMethod(), request.getRequestURI());

        String authHeader = request.getHeader("Authorization");

        // Pas de Bearer → anonyme
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.debug("[SEC] Pas de Bearer → anonyme");
            filterChain.doFilter(request, response);
            return;
        }

        // Récupération du token sans le préfixe
        String token = authHeader.substring(7).trim();

        // Cas fréquents en front : "null" / "undefined"
        if (!StringUtils.hasText(token)
                || "null".equalsIgnoreCase(token)
                || "undefined".equalsIgnoreCase(token)) {
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
                log.debug("[SEC] Auth déjà présente → skip setAuthentication");
                filterChain.doFilter(request, response);
                return;
            }

            // Valider le token (signature/expiration/sujet)
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
            log.debug("[SEC] Utilisateur trouvé: {}", user.getNom());

            // --- Normalisation du rôle (String ou Enum) ---
            String rawRole = null;
            try {
                Object roleObj = user.getRole(); // String OU Enum selon ta version
                rawRole = (roleObj == null) ? null : roleObj.toString();
            } catch (Exception ignored) {}
            String role = (rawRole == null || rawRole.isBlank()) ? "PARENT" : rawRole.trim().toUpperCase();
            if (role.startsWith("ROLE_")) role = role.substring(5); // garde "ADMIN" au lieu de "ROLE_ADMIN"
            log.debug("[SEC] Role utilisateur: {}", role);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>(2);
            authorities.add(new SimpleGrantedAuthority(role));            // ex: "ADMIN"
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));  // ex: "ROLE_ADMIN"

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("[SEC] Auth OK: user={} roles={}", email, authorities);

        } catch (Exception ex) {
            // En cas d'erreur, on nettoie le contexte et on laisse passer → Security/PreAuthorize tranchera.
            SecurityContextHolder.clearContext();
            log.warn("[SEC] Erreur parsing/validation JWT: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}