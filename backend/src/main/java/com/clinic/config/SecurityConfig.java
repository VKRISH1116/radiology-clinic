package com.clinic.config;

import com.clinic.auth.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Let the servlet ERROR dispatch through, otherwise a controller
                        // that throws (400/401/409) gets re-checked here and masked as 403.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
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
     * BCrypt: a deliberately slow, salted password hash. Same raw password yields
     * a different hash each time (random salt), and matches() re-derives to compare.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
