package com.clinic.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One study (scan) on an appointment — a line item. Maps to "appointment_studies".
 *
 * The key field is price_snapshot: the service's price COPIED at booking time
 * (rule BR-9). Editing a service's catalogue price later must not change what a
 * past visit was billed, so we never read the live price for an old appointment.
 */
@Entity
@Table(name = "appointment_studies")
public class AppointmentStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The owning appointment. LAZY so listing studies doesn't re-load the parent.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "price_snapshot", nullable = false)
    private BigDecimal priceSnapshot;

    public AppointmentStudy() {
        // JPA requires a no-arg constructor.
    }

    public AppointmentStudy(Long serviceId, BigDecimal priceSnapshot) {
        this.serviceId = serviceId;
        this.priceSnapshot = priceSnapshot;
    }

    public Long getId() {
        return id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }
}
