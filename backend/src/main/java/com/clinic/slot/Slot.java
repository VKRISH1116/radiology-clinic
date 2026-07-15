package com.clinic.slot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * A bookable 15-minute time slot. Maps to the "slots" table (Flyway V1).
 *
 * start_time is UNIQUE, so the grid never has two slots at the same instant.
 * capacity is how many appointments the slot can hold (default 1); the booking
 * logic refuses to exceed it, which is how we enforce "no double-booking".
 */
@Entity
@Table(name = "slots")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false, unique = true)
    private OffsetDateTime startTime;

    @Column(nullable = false)
    private int capacity;

    public Slot() {
        // JPA requires a no-arg constructor.
    }

    public Slot(OffsetDateTime startTime, int capacity) {
        this.startTime = startTime;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public int getCapacity() {
        return capacity;
    }
}
