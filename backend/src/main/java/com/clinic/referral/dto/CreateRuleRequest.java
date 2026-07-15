package com.clinic.referral.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Admin request to add a referral rule. Any scope field left null means "any"
 * (widens the rule); percentage is the only required field.
 */
public record CreateRuleRequest(
        Long referringDoctorId,
        Long serviceId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percentage,
        Boolean active) {
}
