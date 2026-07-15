package com.clinic.referral.dto;

import java.math.BigDecimal;

/** Per-doctor payout totals — the "top referrers" report, biggest payout first. */
public record ReferralSummaryResponse(
        Long referringDoctorId,
        String doctorName,
        long referralCount,
        BigDecimal totalAmount) {
}
