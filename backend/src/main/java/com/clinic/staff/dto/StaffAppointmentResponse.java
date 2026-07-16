package com.clinic.staff.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * An appointment as the staff schedule shows it: adds the patient's name and
 * whether a report is attached (fields the patient's own view doesn't need).
 */
public record StaffAppointmentResponse(
        Long id,
        String patientName,
        OffsetDateTime slotStartTime,
        String status,
        BigDecimal billedAmount,
        List<StudyLine> studies,
        String reportFileName) {

    public record StudyLine(Long serviceId, String name, BigDecimal priceSnapshot) {
    }
}
