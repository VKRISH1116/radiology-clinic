package com.clinic.referral.dto;

import com.clinic.referral.ReferralRule;
import java.math.BigDecimal;

/** A configured referral rule, as shown in the admin rule list. */
public record RuleResponse(
        Long id,
        Long referringDoctorId,
        Long serviceId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal percentage,
        boolean active) {

    public static RuleResponse from(ReferralRule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getReferringDoctorId(),
                rule.getServiceId(),
                rule.getMinAmount(),
                rule.getMaxAmount(),
                rule.getPercentage(),
                rule.isActive());
    }
}
