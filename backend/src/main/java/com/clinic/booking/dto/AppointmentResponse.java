package com.clinic.booking.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Confirmation of a booked appointment, including the priced line items. */
public record AppointmentResponse(
        Long id,
        Long patientId,
        Long slotId,
        OffsetDateTime slotStartTime,
        String status,
        BigDecimal billedAmount,
        List<StudyLine> studies) {

    /** One booked study, showing the price captured at booking time. */
    public record StudyLine(Long serviceId, String name, BigDecimal priceSnapshot) {
    }
}
