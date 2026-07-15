package com.clinic.booking.dto;

import java.time.OffsetDateTime;

/**
 * A slot as the booking UI sees it: its time, total capacity, and how many seats
 * are still free (capacity minus live appointments). available == 0 means booked.
 */
public record SlotAvailabilityResponse(
        Long id,
        OffsetDateTime startTime,
        int capacity,
        long available) {
}
