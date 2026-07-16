package com.clinic.config;

import com.clinic.auth.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Central security policy for the API.
 *
 * Key choices (see docs/Architecture.html ADR "JWT stateless auth"):
 *  - STATELESS: no HTTP session; every request must carry its own JWT.
 *  - CSRF disabled: CSRF defends cookie/session auth; we use bearer tokens, not cookies.
 *  - /api/auth/** open (register/login must work before you have a token);
 *    everything else requires authentication.
 *  - Our JwtAuthenticationFilter runs before the username/password filter so a
 *    valid token authenticates the request.
 */
@Configuration
@EnableWebSecurity
// Turns on @PreAuthorize/@PostAuthorize so controllers can gate methods by role
// (e.g. staff/admin-only actions). Roles come from the JWT as ROLE_* authorities.
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Allow the browser SPA (a different origin) to call the API; the
                // actual policy comes from the corsConfigurationSource bean below.
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Let the servlet ERROR dispatch through, otherwise a controller
                        // that throws (400/401/409) gets re-checked here and masked as 403.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/health").permitAll()        // public: uptime monitoring
                        .requestMatchers("/h2-console/**").permitAll() // dev DB console (h2 profile)
                        .anyRequest().authenticated())
                // Missing/invalid token -> 401 Unauthorized (the default here would be 403).
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // The H2 console renders inside frames; allow same-origin framing for it.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS policy for the browser SPA. The browser sends a preflight OPTIONS for
     * cross-origin calls; this tells it which origins/methods/headers are allowed.
     * Allowed origins come from config (app.cors.allowed-origins), so prod can add
     * its real domain. We use bearer tokens (not cookies), so credentials stay off.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * BCrypt: a deliberately slow, salted password hash. Same raw password yields
     * a different hash each time (random salt), and matches() re-derives to compare.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
