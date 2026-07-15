package com.clinic.booking;

import com.clinic.booking.dto.AppointmentResponse;
import com.clinic.booking.dto.BookAppointmentRequest;
import com.clinic.booking.dto.SlotAvailabilityResponse;
import com.clinic.catalog.Service;
import com.clinic.catalog.ServiceRepository;
import com.clinic.patient.Patient;
import com.clinic.patient.PatientRepository;
import com.clinic.slot.Slot;
import com.clinic.slot.SlotRepository;
import com.clinic.slot.SlotService;
import com.clinic.user.User;
import com.clinic.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Booking logic: browse a day's availability, and create an appointment.
 *
 * The class is fully-qualified as {@code @org.springframework.stereotype.Service}
 * because the catalogue entity we import is also named {@link Service}, so the
 * simple name "Service" would otherwise be ambiguous in this file.
 */
@org.springframework.stereotype.Service
public class BookingService {

    private final SlotService slotService;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public BookingService(
            SlotService slotService,
            SlotRepository slotRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ServiceRepository serviceRepository,
            UserRepository userRepository) {
        this.slotService = slotService;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    /** A day's slots with remaining availability (generating the day if needed). */
    @Transactional
    public List<SlotAvailabilityResponse> listAvailableSlots(LocalDate date) {
        slotService.ensureDayExists(date);
        List<Slot> slots = slotService.listDay(date);
        if (slots.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> bookedBySlot = new LinkedHashMap<>();
        List<Long> slotIds = slots.stream().map(Slot::getId).toList();
        for (Object[] row : appointmentRepository.countActiveGroupedBySlotId(slotIds)) {
            bookedBySlot.put((Long) row[0], (Long) row[1]);
        }

        return slots.stream()
                .map(slot -> new SlotAvailabilityResponse(
                        slot.getId(),
                        slot.getStartTime(),
                        slot.getCapacity(),
                        slot.getCapacity() - bookedBySlot.getOrDefault(slot.getId(), 0L)))
                .toList();
    }

    /**
     * Book a slot for one or more studies. Snapshots each study's current price,
     * bills their sum, and refuses to exceed the slot's capacity.
     *
     * @param userEmail the authenticated user (from the JWT subject) booking for themselves
     */
    @Transactional
    public AppointmentResponse book(BookAppointmentRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Unknown user"));

        Patient patient = findOrCreatePatient(user, request.patient());

        // Lock the slot row for this transaction so concurrent bookings on the same
        // slot are serialized — this is what makes the capacity check race-free.
        Slot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Slot not found"));

        long alreadyBooked = appointmentRepository
                .countBySlotIdAndStatusNot(slot.getId(), AppointmentStatus.CANCELLED);
        if (alreadyBooked >= slot.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is fully booked");
        }

        // De-duplicate study ids (booking the same study twice is meaningless) and
        // load them, validating each exists and is currently bookable.
        List<Long> serviceIds = new LinkedHashSet<>(request.serviceIds()).stream().toList();
        Map<Long, Service> servicesById = new LinkedHashMap<>();
        serviceRepository.findAllById(serviceIds)
                .forEach(service -> servicesById.put(service.getId(), service));

        Appointment appointment = new Appointment();
        appointment.setPatientId(patient.getId());
        appointment.setSlotId(slot.getId());
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setCreatedAt(OffsetDateTime.now());
        appointment.setCreatedBy(user.getId());

        BigDecimal bill = BigDecimal.ZERO;
        for (Long serviceId : serviceIds) {
            Service service = servicesById.get(serviceId);
            if (service == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown service: " + serviceId);
            }
            if (!service.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Service not bookable: " + service.getName());
            }
            // Snapshot the price NOW (BR-9) so later catalogue edits don't change this bill.
            appointment.addStudy(new AppointmentStudy(service.getId(), service.getPrice()));
            bill = bill.add(service.getPrice());
        }
        appointment.setBilledAmount(bill);

        appointmentRepository.save(appointment); // cascade persists the studies too
        return toResponse(appointment, slot, servicesById);
    }

    /** Reuse the user's patient profile, or create one from the booking details. */
    private Patient findOrCreatePatient(User user, BookAppointmentRequest.PatientDetails details) {
        return patientRepository.findByUserId(user.getId()).orElseGet(() -> {
            Patient patient = new Patient();
            patient.setUserId(user.getId());
            patient.setFullName(details.fullName());
            patient.setPhone(details.phone());
            patient.setDob(details.dob());
            patient.setGender(details.gender());
            patient.setCreatedAt(OffsetDateTime.now());
            return patientRepository.save(patient);
        });
    }

    private AppointmentResponse toResponse(
            Appointment appointment, Slot slot, Map<Long, Service> servicesById) {
        List<AppointmentResponse.StudyLine> lines = appointment.getStudies().stream()
                .map(study -> new AppointmentResponse.StudyLine(
                        study.getServiceId(),
                        servicesById.get(study.getServiceId()).getName(),
                        study.getPriceSnapshot()))
                .toList();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                slot.getId(),
                slot.getStartTime(),
                appointment.getStatus().name(),
                appointment.getBilledAmount(),
                lines);
    }
}
