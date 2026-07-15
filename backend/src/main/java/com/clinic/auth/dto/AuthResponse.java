package com.clinic.auth.dto;

/** Returned by a successful login: the bearer token plus who it belongs to. */
public record AuthResponse(String token, String email, String role) {
}
