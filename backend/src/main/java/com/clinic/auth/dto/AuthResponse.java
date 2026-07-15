package com.clinic.auth.dto;

/**
 * Returned by login and refresh: a short-lived access token (the JWT, field kept
 * as "token"), a long-lived refresh token to obtain new access tokens, and who
 * the tokens belong to.
 */
public record AuthResponse(String token, String refreshToken, String email, String role) {
}
