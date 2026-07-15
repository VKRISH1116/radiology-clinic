package com.clinic.booking.dto;

import jakarta.validation.constraints.NotNull;

/** Body for POST /api/appointments/{id}/reschedule — the slot to move to. */
public record RescheduleRequest(@NotNull Long slotId) {
}
