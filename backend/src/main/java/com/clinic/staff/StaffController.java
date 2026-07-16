package com.clinic.staff;

import com.clinic.staff.dto.StaffAppointmentResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The clinic-wide appointment schedule, for staff/admin. */
@RestController
@RequestMapping("/api/appointments")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<StaffAppointmentResponse> schedule() {
        return staffService.schedule();
    }
}
