package com.clinic.booking;

/** Matches CHECK (status IN ('BOOKED','IN_PROGRESS','COMPLETED','CANCELLED')). */
public enum AppointmentStatus {
    BOOKED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
