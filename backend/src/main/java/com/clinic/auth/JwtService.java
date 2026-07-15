package com.clinic.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies JSON Web Tokens (HS256, symmetric key).
 *
 * A JWT is three base64 parts: header.payload.signature. The payload carries
 * claims (subject = email, a custom "role", issued/expiry timestamps). The
 * signature is an HMAC of header+payload using our secret key, so anyone can
 * READ a token but only the server (holding the key) can MINT or TAMPER one.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // hmacShaKeyFor requires >= 32 bytes for HS256; enforced by our config default.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Mint a signed token for a logged-in user. */
    public String generateToken(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * Verify the signature and expiry, returning the claims.
     * Throws a JwtException (unchecked) if the token is invalid/expired/tampered.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
