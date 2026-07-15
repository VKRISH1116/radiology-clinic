package com.clinic.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Admin request to edit a catalogue entry. Editing the price does NOT change past
 * appointments — they snapshot the price at booking (rule BR-9) — so this only
 * affects future bookings.
 */
public record UpdateServiceRequest(
        @NotBlank String category,
        @NotBlank String name,
        @NotNull @DecimalMin("0") BigDecimal price,
        @NotNull Boolean active) {
}
