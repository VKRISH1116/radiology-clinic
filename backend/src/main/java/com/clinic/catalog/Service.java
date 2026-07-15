package com.clinic.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One entry in the clinic's service catalogue — a scan the patient can book
 * (e.g. "Ultrasound Thyroid", 1500). Maps to the "services" table created by
 * Flyway V1 and seeded by V2.
 *
 * As with {@code User}, JPA runs with ddl-auto=validate, so these fields must
 * line up with the table exactly or startup fails — a cheap schema guard.
 *
 * Money is a {@link BigDecimal} (mapping NUMERIC(10,2)), never double: doubles
 * can't represent values like 0.10 exactly, and prices/bills must be exact.
 */
@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB assigns the id (BIGSERIAL / identity)
    private Long id;

    // Groups scans in the UI, e.g. "General" vs "Obstetrics".
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    // Catalogue entries are deactivated, never deleted, so past appointments that
    // reference them stay intact. Only active services are offered for booking.
    @Column(nullable = false)
    private boolean active;

    public Service() {
        // JPA requires a no-arg constructor.
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
