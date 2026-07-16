package com.clinic.admin;

import com.clinic.admin.dto.DashboardStatsResponse;
import com.clinic.booking.AppointmentRepository;
import com.clinic.patient.PatientRepository;
import com.clinic.referral.ReferralService;
import com.clinic.referral.dto.ReferralSummaryResponse;
import com.clinic.report.ReportRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregate over several features for the admin overview. Lives in its
 * own {@code admin} package (rather than any one feature) because it composes
 * appointments, patients, reports and referrals — keeping those features free of
 * dashboard-shaped coupling, the same reason the staff schedule got its own pkg.
 */
@Service
public class AdminStatsService {

    // Count "today" against the clinic's wall clock, not the server's timezone.
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final int TOP_REFERRERS = 5;

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ReportRepository reportRepository;
    private final ReferralService referralService;

    public AdminStatsService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ReportRepository reportRepository,
            ReferralService referralService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.reportRepository = reportRepository;
        this.referralService = referralService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse compute() {
        LocalDate today = LocalDate.now(ZONE);
        OffsetDateTime dayStart = today.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime nextDayStart = today.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();

        long todaysAppointments =
                appointmentRepository.countActiveInSlotWindow(dayStart, nextDayStart);
        long totalPatients = patientRepository.count();
        long reportsDelivered = reportRepository.count();
        long reportsPending = appointmentRepository.countCompletedWithoutReport();

        // summariseByDoctor is already biggest-payout-first; keep the leaders.
        List<ReferralSummaryResponse> topReferrers =
                referralService.summariseByDoctor().stream().limit(TOP_REFERRERS).toList();

        return new DashboardStatsResponse(
                todaysAppointments, totalPatients, reportsDelivered, reportsPending, topReferrers);
    }
}
