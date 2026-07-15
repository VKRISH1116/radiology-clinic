package com.clinic.consent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    boolean existsByPatientId(Long patientId);
}
