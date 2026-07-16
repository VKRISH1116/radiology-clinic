package com.clinic.doctor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A doctor who refers patients to the clinic. Maps to "referring_doctors".
 *
 * Referring doctors do NOT log in (MVP decision): they are reference data used to
 * tag an appointment and drive the referral payout. Deactivated, never deleted,
 * so historical referrals keep pointing at a valid row.
 */
@Entity
@Table(name = "referring_doctors")
public class ReferringDoctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Column(nullable = false)
    private boolean active;

    public ReferringDoctor() {
        // JPA requires a no-arg constructor.
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
