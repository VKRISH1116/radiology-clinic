package com.clinic.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Admin request to add a catalogue entry. New services default to active. */
public record CreateServiceRequest(
        @NotBlank String category,
        @NotBlank String name,
        @NotNull @DecimalMin("0") BigDecimal price,
        Boolean active) {
}
