package com.clinic.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/auth/register. Bean Validation annotations are enforced
 * by @Valid in the controller, so bad input fails fast with 400 before any logic.
 *
 * There is deliberately NO role field: public self-registration ALWAYS creates a
 * PATIENT. Staff/admin accounts are created by an admin via /api/admin/users, so
 * nobody can grant themselves elevated access.
 */
public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password) {
}
