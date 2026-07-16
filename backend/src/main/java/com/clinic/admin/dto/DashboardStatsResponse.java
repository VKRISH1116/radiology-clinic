package com.clinic.admin.dto;

import com.clinic.referral.dto.ReferralSummaryResponse;
import java.util.List;

/**
 * The admin dashboard's at-a-glance figures (AC-F7-1): today's live appointments,
 * how many patients exist, how many reports are delivered vs still pending, and
 * the top-5 referring doctors by payout.
 */
public record DashboardStatsResponse(
        long todaysAppointments,
        long totalPatients,
        long reportsDelivered,
        long reportsPending,
        List<ReferralSummaryResponse> topReferrers) {
}
