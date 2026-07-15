package com.clinic.referral;

import com.clinic.referral.dto.CreateRuleRequest;
import com.clinic.referral.dto.ReferralResponse;
import com.clinic.referral.dto.ReferralSummaryResponse;
import com.clinic.referral.dto.RuleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Referral payout reporting and rule configuration. These are back-office
 * endpoints: viewing payouts is STAFF/ADMIN; editing the rule engine is ADMIN
 * only. The @PreAuthorize checks are enforced by method security (SecurityConfig).
 */
@RestController
@RequestMapping("/api")
public class ReferralController {

    private final ReferralService referralService;
    private final ReferralRuleRepository ruleRepository;

    public ReferralController(
            ReferralService referralService, ReferralRuleRepository ruleRepository) {
        this.referralService = referralService;
        this.ruleRepository = ruleRepository;
    }

    /** The payout ledger, newest first. */
    @GetMapping("/referrals")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ReferralResponse> referrals() {
        return referralService.listReferrals();
    }

    /** Per-doctor totals (top referrers). */
    @GetMapping("/referrals/summary")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<ReferralSummaryResponse> summary() {
        return referralService.summariseByDoctor();
    }

    /** Mark a referral paid out (ADMIN). */
    @PostMapping("/referrals/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ReferralResponse pay(
            @PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        return referralService.markPaid(id, authentication.getName());
    }

    @GetMapping("/referral-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RuleResponse> rules() {
        return ruleRepository.findAllByOrderByIdAsc().stream()
                .map(RuleResponse::from)
                .toList();
    }

    @PostMapping("/referral-rules")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse createRule(@Valid @RequestBody CreateRuleRequest request) {
        ReferralRule rule = new ReferralRule();
        rule.setReferringDoctorId(request.referringDoctorId());
        rule.setServiceId(request.serviceId());
        rule.setMinAmount(request.minAmount());
        rule.setMaxAmount(request.maxAmount());
        rule.setPercentage(request.percentage());
        rule.setActive(request.active() == null ? true : request.active());
        return RuleResponse.from(ruleRepository.save(rule));
    }
}
