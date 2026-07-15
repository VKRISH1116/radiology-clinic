package com.clinic.referral.dto;

import com.clinic.referral.Referral;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** A ledger row for the payout report. */
public record ReferralResponse(
        Long id,
        Long appointmentId,
        Long referringDoctorId,
        String doctorName,
        Long ruleId,
        BigDecimal amount,
        String status,
        OffsetDateTime computedAt) {

    public static ReferralResponse of(Referral referral, String doctorName) {
        return new ReferralResponse(
                referral.getId(),
                referral.getAppointmentId(),
                referral.getReferringDoctorId(),
                doctorName,
                referral.getRuleId(),
                referral.getAmount(),
                referral.getStatus().name(),
                referral.getComputedAt());
    }
}
