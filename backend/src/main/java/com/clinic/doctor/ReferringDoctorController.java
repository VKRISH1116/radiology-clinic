package com.clinic.doctor;

import com.clinic.doctor.dto.ReferringDoctorResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lists active referring doctors so a booking can be tagged with one. */
@RestController
@RequestMapping("/api/referring-doctors")
public class ReferringDoctorController {

    private final ReferringDoctorRepository referringDoctorRepository;

    public ReferringDoctorController(ReferringDoctorRepository referringDoctorRepository) {
        this.referringDoctorRepository = referringDoctorRepository;
    }

    @GetMapping
    public List<ReferringDoctorResponse> list() {
        return referringDoctorRepository.findByActiveTrueOrderByName().stream()
                .map(ReferringDoctorResponse::from)
                .toList();
    }
}
