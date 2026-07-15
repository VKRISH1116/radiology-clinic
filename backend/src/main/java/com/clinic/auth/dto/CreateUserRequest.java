package com.clinic.auth.dto;

import com.clinic.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/admin/users (ADMIN only). Unlike public registration, this
 * lets an admin set the role — the controlled path for creating STAFF/ADMIN.
 */
public record CreateUserRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        @NotNull Role role) {
}
