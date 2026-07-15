package com.clinic.auth;

import com.clinic.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Issues, rotates, and revokes refresh tokens.
 *
 * A refresh token is an opaque random string handed to the client. Only its
 * SHA-256 hash is stored, so we can recognise it later without keeping a
 * replayable secret. On use we ROTATE (revoke the old, issue a new) so a stolen
 * token has a short useful life; logout simply revokes.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /** Mint a new refresh token for a user and persist its hash. Returns the RAW token. */
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)));
        entity.setRevoked(false);
        entity.setCreatedAt(OffsetDateTime.now());
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validate a presented refresh token and rotate it: the old one is revoked and
     * a fresh one issued. Returns the owning user id and the new raw token.
     */
    @Transactional
    public Rotation rotate(String rawToken, java.util.function.LongFunction<User> userLookup) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        current.setRevoked(true);
        refreshTokenRepository.save(current);

        User user = userLookup.apply(current.getUserId());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        return new Rotation(user, issue(user));
    }

    /** Revoke a refresh token (logout). Idempotent: an unknown token is a no-op. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e); // never happens on a JVM
        }
    }

    /** Result of a rotation: the token's owner and the new raw refresh token. */
    public record Rotation(User user, String rawToken) {
    }
}
