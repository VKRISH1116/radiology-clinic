package com.clinic.auth;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A protected endpoint used to prove the token round-trip works: it can only be
 * reached with a valid Bearer token, and it echoes back who the token says you are.
 * The Authentication object is populated by JwtAuthenticationFilter.
 */
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Map.of(
                "email", authentication.getName(),
                "roles", roles);
    }
}
