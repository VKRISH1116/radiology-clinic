package com.clinic.referral;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One configurable rule in the referral engine. Maps to "referral_rules".
 *
 * Each scope column is a filter that means "any" when NULL:
 *   referringDoctorId — this doctor only, or any doctor
 *   serviceId         — bills including this study, or any study
 *   minAmount/maxAmount — bill within these bounds, or unbounded
 * The percentage is applied to the whole bill. When several rules match, the
 * most specific one wins (see {@link ReferralEngine}).
 */
@Entity
@Table(name = "referral_rules")
public class ReferralRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referring_doctor_id")
    private Long referringDoctorId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "min_amount")
    private BigDecimal minAmount;

    @Column(name = "max_amount")
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private BigDecimal percentage;

    @Column(nullable = false)
    private boolean active;

    public ReferralRule() {
        // JPA requires a no-arg constructor.
    }

    public Long getId() {
        return id;
    }

    public Long getReferringDoctorId() {
        return referringDoctorId;
    }

    public void setReferringDoctorId(Long referringDoctorId) {
        this.referringDoctorId = referringDoctorId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
