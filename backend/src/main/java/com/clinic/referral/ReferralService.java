package com.clinic.referral;

import com.clinic.audit.AuditService;
import com.clinic.doctor.ReferringDoctorRepository;
import com.clinic.referral.ReferralEngine.ReferralOutcome;
import com.clinic.referral.dto.ReferralResponse;
import com.clinic.referral.dto.ReferralSummaryResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Records referral payouts (when a referred visit completes) and reports on them.
 * The scoring itself lives in {@link ReferralEngine}; this class owns persistence
 * and the read models.
 */
@Service
public class ReferralService {

    private final ReferralEngine referralEngine;
    private final ReferralRepository referralRepository;
    private final ReferringDoctorRepository referringDoctorRepository;
    private final AuditService auditService;

    public ReferralService(
            ReferralEngine referralEngine,
            ReferralRepository referralRepository,
            ReferringDoctorRepository referringDoctorRepository,
            AuditService auditService) {
        this.referralEngine = referralEngine;
        this.referralRepository = referralRepository;
        this.referringDoctorRepository = referringDoctorRepository;
        this.auditService = auditService;
    }

    /**
     * Compute and persist the referral for a just-completed visit. No-ops when the
     * visit had no referring doctor, or a payout was already recorded (idempotent —
     * the ledger's UNIQUE appointment_id means a visit is paid at most once).
     */
    @Transactional
    public Optional<Referral> recordFor(
            Long appointmentId, Long doctorId, Set<Long> serviceIds, BigDecimal bill) {
        if (doctorId == null || referralRepository.existsByAppointmentId(appointmentId)) {
            return Optional.empty();
        }
        Optional<ReferralOutcome> outcome = referralEngine.evaluate(doctorId, serviceIds, bill);
        if (outcome.isEmpty()) {
            return Optional.empty(); // no matching rule (the default 20% normally catches all)
        }

        ReferralOutcome result = outcome.get();
        Referral referral = new Referral();
        referral.setAppointmentId(appointmentId);
        referral.setReferringDoctorId(doctorId);
        referral.setRuleId(result.rule().getId());
        referral.setAmount(result.amount());
        referral.setStatus(ReferralStatus.PENDING);
        referral.setComputedAt(OffsetDateTime.now());
        return Optional.of(referralRepository.save(referral));
    }

    /** Mark a referral paid out (admin action). 409 if it was already paid. */
    @Transactional
    public ReferralResponse markPaid(Long referralId, String actorEmail) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Referral not found"));
        if (referral.getStatus() == ReferralStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Referral already paid");
        }
        referral.setStatus(ReferralStatus.PAID);
        referralRepository.save(referral);
        auditService.record("PAYOUT_UPDATE", "referral", referral.getId(), actorEmail);
        return ReferralResponse.of(referral, doctorNames().get(referral.getReferringDoctorId()));
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> listReferrals() {
        Map<Long, String> doctorNames = doctorNames();
        return referralRepository.findAllByOrderByComputedAtDesc().stream()
                .map(referral -> ReferralResponse.of(
                        referral, doctorNames.get(referral.getReferringDoctorId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReferralSummaryResponse> summariseByDoctor() {
        Map<Long, String> doctorNames = doctorNames();
        return referralRepository.summariseByDoctor().stream()
                .map(row -> new ReferralSummaryResponse(
                        (Long) row[0],
                        doctorNames.get((Long) row[0]),
                        ((Number) row[1]).longValue(),
                        new BigDecimal(row[2].toString())))
                .toList();
    }

    private Map<Long, String> doctorNames() {
        Map<Long, String> names = new LinkedHashMap<>();
        referringDoctorRepository.findAll().forEach(doctor -> names.put(doctor.getId(), doctor.getName()));
        return names;
    }
}
