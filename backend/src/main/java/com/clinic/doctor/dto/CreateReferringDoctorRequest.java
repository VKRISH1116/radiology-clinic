package com.clinic.doctor.dto;

import jakarta.validation.constraints.NotBlank;

/** Create a referring doctor on the fly from the booking screen. Phone optional. */
public record CreateReferringDoctorRequest(
        @NotBlank String name,
        String phone) {
}
