package com.clinic.consent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * A record that a patient consented to a given version of the privacy/consent
 * terms (NFR-02). Maps to "consent_records". Captured when the patient profile is
 * created; keeping the version means we can prove WHAT was agreed to and WHEN.
 */
@Entity
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "consent_version", nullable = false)
    private String consentVersion;

    @Column(name = "consented_at", nullable = false)
    private OffsetDateTime consentedAt;

    public ConsentRecord() {
        // JPA requires a no-arg constructor.
    }

    public ConsentRecord(Long patientId, String consentVersion, OffsetDateTime consentedAt) {
        this.patientId = patientId;
        this.consentVersion = consentVersion;
        this.consentedAt = consentedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getConsentVersion() {
        return consentVersion;
    }

    public OffsetDateTime getConsentedAt() {
        return consentedAt;
    }
}
