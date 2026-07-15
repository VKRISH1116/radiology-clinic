package com.clinic.booking.dto;

import com.clinic.patient.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * A booking request: which slot, which studies, and (for a first-time patient)
 * who the patient is. serviceIds may bundle several studies into one visit.
 */
public record BookAppointmentRequest(
        @NotNull Long slotId,
        @NotEmpty List<Long> serviceIds,
        @Valid @NotNull PatientDetails patient) {

    /**
     * Minimal patient identity, used to create the patient profile the first time
     * this user books. On later bookings an existing profile is reused and these
     * values are ignored. fullName is the only hard requirement (NOT NULL in DB).
     */
    public record PatientDetails(
            @NotBlank String fullName,
            String phone,
            LocalDate dob,
            Gender gender) {
    }
}
