package com.clinic.booking;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The "visit" a patient books. Maps to the "appointments" table (Flyway V1).
 *
 * This is the aggregate root: it owns its {@link AppointmentStudy} line items via
 * a cascade, so saving the appointment saves its studies in one transaction, and
 * they share its lifecycle (appointment_studies has ON DELETE CASCADE).
 *
 * Related rows (patient, slot, referring doctor, creator) are held as plain id
 * fields rather than JPA associations — they belong to other features, and scalar
 * ids keep this entity a simple, loosely-coupled record.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "referring_doctor_id")
    private Long referringDoctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    // Sum of the studies' price snapshots at booking time.
    @Column(name = "billed_amount")
    private BigDecimal billedAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentStudy> studies = new ArrayList<>();

    public Appointment() {
        // JPA requires a no-arg constructor.
    }

    /** Add a line item and keep both sides of the relationship in sync. */
    public void addStudy(AppointmentStudy study) {
        study.setAppointment(this);
        studies.add(study);
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Long getReferringDoctorId() {
        return referringDoctorId;
    }

    public void setReferringDoctorId(Long referringDoctorId) {
        this.referringDoctorId = referringDoctorId;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public BigDecimal getBilledAmount() {
        return billedAmount;
    }

    public void setBilledAmount(BigDecimal billedAmount) {
        this.billedAmount = billedAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public List<AppointmentStudy> getStudies() {
        return studies;
    }
}
