package com.clinic.doctor.dto;

import com.clinic.doctor.ReferringDoctor;

/** A referring doctor as offered in the booking UI. */
public record ReferringDoctorResponse(Long id, String name, String phone) {

    public static ReferringDoctorResponse from(ReferringDoctor doctor) {
        return new ReferringDoctorResponse(doctor.getId(), doctor.getName(), doctor.getPhone());
    }
}
