package com.clinic.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test for JwtService — no Spring context, no database, so it runs in
 * milliseconds. Verifies the two things a JWT must guarantee: a token we mint
 * reads back unchanged, and any token we did NOT validly sign is rejected.
 */
class JwtServiceTest {

    // HS256 needs a key of at least 32 bytes; these test secrets satisfy that.
    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes";
    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    @Test
    void generatedTokenParsesBackToSameClaims() {
        String token = jwtService.generateToken("alice@clinic.test", "ADMIN");

        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("alice@clinic.test");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("alice@clinic.test", "ADMIN");

        // Appending to the token corrupts the signature segment.
        assertThatThrownBy(() -> jwtService.parseClaims(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithADifferentKey() {
        JwtService attacker = new JwtService("a-totally-different-secret-key-32-bytes-x", 60_000);
        String forged = attacker.generateToken("mallory@clinic.test", "ADMIN");

        // Our service holds a different key, so the signature won't verify.
        assertThatThrownBy(() -> jwtService.parseClaims(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        // Negative lifetime => the token's expiry is already in the past when minted.
        JwtService expiring = new JwtService(SECRET, -1_000);
        String token = expiring.generateToken("alice@clinic.test", "PATIENT");

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
