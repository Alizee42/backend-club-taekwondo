package club.taekwondo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthRateLimitFilter authRateLimitFilter,
                                                   JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 🔓 Actuator health — public pour CI/CD et monitoring
                .requestMatchers("/actuator/health").permitAll()
                // 🔓 Swagger / OpenAPI — public (désactivé en prod via springdoc.*.enabled=false)
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 🔓 Endpoints publics
                .requestMatchers(HttpMethod.GET, "/api/galeries/club/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/horaires/club/**").permitAll()
                // Reçus Stripe: autoriser l'ouverture dans un nouvel onglet (pas d'en-tête Authorization transmis)
                .requestMatchers(HttpMethod.GET, "/api/stripe/receipt/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/paiements/*/facture").permitAll()
                .requestMatchers(
                    "/error",
                    "/uploads/**",
                    "/api/uploads/**",
                    "/api/stripe/webhook",
                    "/api/stripe/public-key",
                    "/api/stripe/config-status",
                    "/api/utilisateurs/login",
                    "/api/utilisateurs/register",
                    "/api/parametres-paiement/public/**",
                    "/api/public/**",
                    "/api/galeries/club/**",
                    "/api/enseignants/club/**",
                    "/api/reinitialisation/**"
                ).permitAll()
                // Lecture publique : actualités, avis, produits, événements, hero
                .requestMatchers(HttpMethod.GET,
                    "/api/clubs/**",
                    "/api/actualites/**",
                    "/api/avis/**",
                    "/api/produits/**",
                    "/api/evenements",
                    "/api/evenements/actifs",
                    "/api/evenements/{id}",
                    "/api/evenements/club/*/actifs",
                    "/api/hero-config",
                    "/api/about-config",
                    "/api/mentions-legales-config",
                    "/api/politique-confidentialite-config"
                ).permitAll()
                // Restreindre la modification des clubs (création / modification / suppression)
                .requestMatchers(HttpMethod.POST, "/api/clubs/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/clubs/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/clubs/**").hasRole("SUPER_ADMIN")
                // Enseignants: création / modification / suppression restreintes
                .requestMatchers(HttpMethod.POST, "/api/enseignants/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/enseignants/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/enseignants/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Permettre la création de membres lors de l'inscription (sans auth)
                .requestMatchers(HttpMethod.POST, "/api/membres").permitAll()

                // 👥 Membres : MEMBRE, PARENT, ADMIN, SUPER_ADMIN (pour les autres opérations)
                .requestMatchers("/api/membres/**").hasAnyRole("MEMBRE", "PARENT", "ADMIN", "SUPER_ADMIN")

                // 👑 Admin uniquement
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 🔒 Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )
            .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                // 401 si pas de token / token invalide
                .authenticationEntryPoint((req, res, e) -> {
                    log.warn("[SEC] 401 {} {}", req.getMethod(), req.getRequestURI());
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"message\":\"Unauthorized\"}");
                })
                // 403 si token valide mais rôle insuffisant
                .accessDeniedHandler((req, res, e) -> {
                    log.warn("[SEC] 403 {} {}", req.getMethod(), req.getRequestURI());
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"message\":\"Access Denied\"}");
                })
            );

        return http.build();
    }

    // 🌍 CORS : Angular local + Netlify (prod + previews)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowCredentials(true);
        c.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200",
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://frontend-club-taekwondo.netlify.app",
            "https://club-taekwondo-*.netlify.app"
        ));

        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Location", "Authorization"));

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
