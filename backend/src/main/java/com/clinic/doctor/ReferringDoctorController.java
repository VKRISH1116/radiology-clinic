package com.clinic.doctor;

import com.clinic.doctor.dto.CreateReferringDoctorRequest;
import com.clinic.doctor.dto.ReferringDoctorResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lists referring doctors, and lets staff/admin add a new one when booking. */
@RestController
@RequestMapping("/api/referring-doctors")
public class ReferringDoctorController {

    private final ReferringDoctorRepository referringDoctorRepository;

    public ReferringDoctorController(ReferringDoctorRepository referringDoctorRepository) {
        this.referringDoctorRepository = referringDoctorRepository;
    }

    /** Active doctors, name order — offered as the booking's referrer options. */
    @GetMapping
    public List<ReferringDoctorResponse> list() {
        return referringDoctorRepository.findByActiveTrueOrderByName().stream()
                .map(ReferringDoctorResponse::from)
                .toList();
    }

    /**
     * Add a referring doctor (STAFF/ADMIN). Used by the walk-in screen's
     * "pick-or-type" field when the typed name isn't an existing doctor.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReferringDoctorResponse create(@Valid @RequestBody CreateReferringDoctorRequest request) {
        ReferringDoctor doctor = new ReferringDoctor();
        doctor.setName(request.name().trim());
        String phone = request.phone() == null || request.phone().isBlank()
                ? null : request.phone().trim();
        doctor.setPhone(phone);
        doctor.setActive(true);
        return ReferringDoctorResponse.from(referringDoctorRepository.save(doctor));
    }
}
