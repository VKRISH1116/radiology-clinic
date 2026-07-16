package com.clinic.auth.dto;

/** A user row for the admin user-management list. */
public record UserSummaryResponse(String email, String role) {
}
