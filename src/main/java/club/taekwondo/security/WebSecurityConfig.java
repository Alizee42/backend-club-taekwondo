package club.taekwondo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity // pour @PreAuthorize(...)
public class WebSecurityConfig {

  private final JwtUtil jwtUtil;

  public WebSecurityConfig(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // API REST: pas de CSRF
      .cors(cors -> cors.configurationSource(req -> {
        CorsConfiguration c = new CorsConfiguration();
        // ⚠️ remplace par l’URL réelle de ton front
        c.setAllowedOrigins(List.of("http://localhost:4200", "https://ton-domaine.fr"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","Stripe-Signature"));
        c.setAllowCredentials(true);
        return c;
      }))
      .authorizeHttpRequests(auth -> auth
        // Webhook Stripe: public mais VÉRIFIÉ par signature
        .requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll()
        // Création d'intent / public-key: auth requise côté parent connecté
        .requestMatchers("/api/stripe/**").authenticated()
        // Auth / login endpoints si tu en as :
        .requestMatchers("/api/auth/**").permitAll()

        // Paiements parents (nécessite JWT)
        .requestMatchers("/api/paiements/parent/**").authenticated()

        // Le reste protège par défaut
        .anyRequest().authenticated()
      )
      .addFilterBefore(new JwtAuthFilter(jwtUtil), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  static class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    JwtAuthFilter(JwtUtil jwtUtil){ this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

      String h = request.getHeader("Authorization");
      if (h != null && h.startsWith("Bearer ")) {
        String token = h.substring(7);
        try {
          String email = jwtUtil.extractEmail(token); // tu l’as déjà
          if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Ici, tu peux charger les rôles réels depuis ta BDD si besoin
            var principal = User.withUsername(email).password("N/A").roles("PARENT").build();
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
          }
        } catch (Exception ignored) {}
      }
      chain.doFilter(request, response);
    }
  }
}
