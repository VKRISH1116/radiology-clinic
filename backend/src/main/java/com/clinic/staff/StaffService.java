package com.clinic.staff;

import com.clinic.booking.Appointment;
import com.clinic.booking.AppointmentRepository;
import com.clinic.booking.AppointmentStudy;
import com.clinic.catalog.ServiceRepository;
import com.clinic.patient.PatientRepository;
import com.clinic.report.ReportRepository;
import com.clinic.slot.SlotRepository;
import com.clinic.staff.dto.StaffAppointmentResponse;
import com.clinic.staff.dto.StaffAppointmentResponse.StudyLine;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model for the staff schedule: joins appointments with patient names, slot
 * times, study names and (if any) the attached report. Lives in its own package
 * so it can depend on booking + report + patient + slot + catalog without a cycle.
 */
@Service
public class StaffService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final ServiceRepository serviceRepository;
    private final ReportRepository reportRepository;

    public StaffService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            SlotRepository slotRepository,
            ServiceRepository serviceRepository,
            ReportRepository reportRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.serviceRepository = serviceRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffAppointmentResponse> schedule() {
        List<Appointment> appointments = appointmentRepository.findAllWithStudies();
        if (appointments.isEmpty()) {
            return List.of();
        }

        Map<Long, String> patientNames = new LinkedHashMap<>();
        patientRepository
                .findAllById(appointments.stream().map(Appointment::getPatientId).distinct().toList())
                .forEach(p -> patientNames.put(p.getId(), p.getFullName()));

        Map<Long, OffsetDateTime> slotTimes = new LinkedHashMap<>();
        slotRepository
                .findAllById(appointments.stream().map(Appointment::getSlotId).distinct().toList())
                .forEach(s -> slotTimes.put(s.getId(), s.getStartTime()));

        Map<Long, String> serviceNames = new LinkedHashMap<>();
        serviceRepository
                .findAllById(appointments.stream()
                        .flatMap(a -> a.getStudies().stream())
                        .map(AppointmentStudy::getServiceId)
                        .distinct()
                        .toList())
                .forEach(svc -> serviceNames.put(svc.getId(), svc.getName()));

        Map<Long, String> reportByAppointment = new LinkedHashMap<>();
        reportRepository.findAll()
                .forEach(r -> reportByAppointment.put(r.getAppointmentId(), r.getFilePath()));

        return appointments.stream()
                .map(a -> new StaffAppointmentResponse(
                        a.getId(),
                        patientNames.get(a.getPatientId()),
                        slotTimes.get(a.getSlotId()),
                        a.getStatus().name(),
                        a.getBilledAmount(),
                        a.getStudies().stream()
                                .map(s -> new StudyLine(
                                        s.getServiceId(),
                                        serviceNames.get(s.getServiceId()),
                                        s.getPriceSnapshot()))
                                .toList(),
                        reportByAppointment.get(a.getId())))
                .sorted(Comparator.comparing(
                        StaffAppointmentResponse::slotStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
