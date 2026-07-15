package com.clinic.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/auth/refresh and /logout — carries the raw refresh token. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
