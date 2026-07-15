package com.clinic.user;

/**
 * Account role. Stored as its exact name in the users.role TEXT column,
 * which the DB constrains with CHECK (role IN ('PATIENT','STAFF','ADMIN')).
 */
public enum Role {
    PATIENT,
    STAFF,
    ADMIN
}
