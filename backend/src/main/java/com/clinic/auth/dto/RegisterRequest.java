package com.clinic.auth.dto;

import com.clinic.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/auth/register. Bean Validation annotations are enforced
 * by @Valid in the controller, so bad input fails fast with 400 before any logic.
 * role is optional; the service defaults it to PATIENT when null.
 */
public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        Role role) {
}
