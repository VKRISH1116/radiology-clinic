package com.clinic.patient;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** The patient profile linked to a login account, if one exists yet. */
    Optional<Patient> findByUserId(Long userId);
}
