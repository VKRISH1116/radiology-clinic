package com.clinic.doctor;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferringDoctorRepository extends JpaRepository<ReferringDoctor, Long> {

    List<ReferringDoctor> findByActiveTrueOrderByName();

    /** Used at booking to confirm the chosen doctor exists and is still active. */
    Optional<ReferringDoctor> findByIdAndActiveTrue(Long id);
}
