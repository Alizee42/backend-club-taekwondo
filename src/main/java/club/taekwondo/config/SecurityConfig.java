package club.taekwondo.config;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
                                                   JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 🔓 Endpoints publics
                .requestMatchers(
                    "/api/stripe/webhook",
                    "/api/stripe/public-key",
                    "/api/stripe/create-payment-intent",
                    "/api/utilisateurs/login",
                    "/api/utilisateurs/register",
                    "/api/parametres-paiement/public" // 👈 accès sans JWT
                ).permitAll()
                .requestMatchers("/api/debug/**").permitAll() // utilitaire
                // 🔒 Tout le reste nécessite auth
                .anyRequest().authenticated()
            )
            // ➜ Branche ton filtre JWT AVANT UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((req, res, e) -> {
                    log.warn("[SEC] 403 {} {}", req.getMethod(), req.getRequestURI());
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"message\":\"Access Denied\"}");
                })
            );

        return http.build();
    }

    // 🌍 CORS: Angular local + Netlify
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(
            "http://localhost:4200",               // Angular local
            "https://frontend-club-taekwondo.netlify.app" // Front en prod
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        c.setExposedHeaders(List.of("Location","Authorization"));

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

}
