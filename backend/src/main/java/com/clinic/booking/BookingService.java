package com.clinic.booking;

import com.clinic.booking.dto.AppointmentResponse;
import com.clinic.booking.dto.BookAppointmentRequest;
import com.clinic.booking.dto.SlotAvailabilityResponse;
import com.clinic.catalog.Service;
import com.clinic.catalog.ServiceRepository;
import com.clinic.consent.ConsentRecord;
import com.clinic.consent.ConsentRecordRepository;
import com.clinic.doctor.ReferringDoctorRepository;
import com.clinic.patient.Patient;
import com.clinic.patient.PatientRepository;
import com.clinic.referral.ReferralService;
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
import java.util.Set;
import java.util.stream.Collectors;
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
    private final ReferringDoctorRepository referringDoctorRepository;
    private final ReferralService referralService;
    private final ChangeCutoffPolicy changeCutoffPolicy;
    private final ConsentRecordRepository consentRecordRepository;
    private final String consentVersion;

    public BookingService(
            SlotService slotService,
            SlotRepository slotRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            ServiceRepository serviceRepository,
            UserRepository userRepository,
            ReferringDoctorRepository referringDoctorRepository,
            ReferralService referralService,
            ChangeCutoffPolicy changeCutoffPolicy,
            ConsentRecordRepository consentRecordRepository,
            @org.springframework.beans.factory.annotation.Value("${app.consent.current-version}")
                    String consentVersion) {
        this.slotService = slotService;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.referringDoctorRepository = referringDoctorRepository;
        this.referralService = referralService;
        this.changeCutoffPolicy = changeCutoffPolicy;
        this.consentRecordRepository = consentRecordRepository;
        this.consentVersion = consentVersion;
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
     * Book a slot for one or more studies, for the authenticated patient. Snapshots
     * each study's current price, bills their sum, and refuses to exceed capacity.
     */
    @Transactional
    public AppointmentResponse book(BookAppointmentRequest request, String userEmail) {
        User user = requireUser(userEmail);
        Patient patient = findOrCreatePatient(user, request.patient());
        return createAppointment(patient, request, user.getId());
    }

    /**
     * Staff/back-office booking for a WALK-IN patient with no login account: a fresh
     * patient record (user_id null) is created from the supplied details.
     */
    @Transactional
    public AppointmentResponse bookWalkIn(BookAppointmentRequest request, String staffEmail) {
        User staff = requireUser(staffEmail);
        Patient patient = persistPatientWithConsent(newPatient(null, request.patient()));
        return createAppointment(patient, request, staff.getId());
    }

    /** Shared booking core: lock the slot, check capacity, snapshot prices, save. */
    private AppointmentResponse createAppointment(
            Patient patient, BookAppointmentRequest request, Long creatorUserId) {
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
        appointment.setCreatedBy(creatorUserId);
        appointment.setReferringDoctorId(resolveReferringDoctorId(request.referringDoctorId()));

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

        Map<Long, String> serviceNames = new LinkedHashMap<>();
        servicesById.forEach((id, service) -> serviceNames.put(id, service.getName()));
        return toResponse(appointment, slot, serviceNames);
    }

    /** The current user's own appointments, newest first (empty if they've never booked). */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listMyAppointments(String userEmail) {
        User user = requireUser(userEmail);
        Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
        if (patient == null) {
            return List.of(); // no profile yet -> no appointments
        }

        List<Appointment> appointments =
                appointmentRepository.findByPatientIdWithStudies(patient.getId());
        if (appointments.isEmpty()) {
            return List.of();
        }

        Map<Long, Slot> slotsById = new LinkedHashMap<>();
        List<Long> slotIds = appointments.stream().map(Appointment::getSlotId).distinct().toList();
        slotRepository.findAllById(slotIds).forEach(slot -> slotsById.put(slot.getId(), slot));

        Map<Long, String> serviceNames = loadServiceNames(appointments);
        return appointments.stream()
                .map(appointment ->
                        toResponse(appointment, slotsById.get(appointment.getSlotId()), serviceNames))
                .toList();
    }

    /**
     * Cancel one of the current user's own appointments. Cancelling is a status
     * change (BOOKED -> CANCELLED), not a delete: the record stays for history, and
     * the slot frees up automatically because availability ignores cancelled ones.
     */
    @Transactional
    public AppointmentResponse cancelMyAppointment(Long appointmentId, String userEmail) {
        User user = requireUser(userEmail);
        // If the user has no profile they own no appointments; 404 like any missing one.
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        // Only your own appointment. 404 (not 403) so we don't reveal that another
        // user's appointment id exists.
        if (!appointment.getPatientId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only a booked appointment can be cancelled (current status: "
                            + appointment.getStatus() + ")");
        }

        Slot slot = slotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        changeCutoffPolicy.check(slot.getStartTime());

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        return toResponse(appointment, slot, loadServiceNames(List.of(appointment)));
    }

    /**
     * Move one of the current user's own BOOKED appointments to another slot. The
     * new slot is locked and capacity-checked exactly like a fresh booking; the old
     * slot frees automatically (availability keys off slot_id). Studies, prices and
     * the bill are untouched — only the time changes.
     */
    @Transactional
    public AppointmentResponse rescheduleMyAppointment(
            Long appointmentId, Long newSlotId, String userEmail) {
        User user = requireUser(userEmail);
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!appointment.getPatientId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only a booked appointment can be rescheduled (current status: "
                            + appointment.getStatus() + ")");
        }

        // Can't change an appointment that's already inside the cutoff window.
        Slot currentSlot = slotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        changeCutoffPolicy.check(currentSlot.getStartTime());

        // Moving to the same slot is a no-op (and would wrongly count itself as full).
        if (appointment.getSlotId().equals(newSlotId)) {
            return toResponse(appointment, currentSlot, loadServiceNames(List.of(appointment)));
        }

        Slot newSlot = slotRepository.findByIdForUpdate(newSlotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        long alreadyBooked = appointmentRepository
                .countBySlotIdAndStatusNot(newSlot.getId(), AppointmentStatus.CANCELLED);
        if (alreadyBooked >= newSlot.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is fully booked");
        }

        appointment.setSlotId(newSlot.getId());
        appointmentRepository.save(appointment);
        return toResponse(appointment, newSlot, loadServiceNames(List.of(appointment)));
    }

    private User requireUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    /** service id -> name, for every study across the given appointments (one query). */
    private Map<Long, String> loadServiceNames(List<Appointment> appointments) {
        List<Long> serviceIds = appointments.stream()
                .flatMap(appointment -> appointment.getStudies().stream())
                .map(AppointmentStudy::getServiceId)
                .distinct()
                .toList();
        Map<Long, String> names = new LinkedHashMap<>();
        serviceRepository.findAllById(serviceIds)
                .forEach(service -> names.put(service.getId(), service.getName()));
        return names;
    }

    /** Reuse the user's patient profile, or create one (with consent) on first booking. */
    private Patient findOrCreatePatient(User user, BookAppointmentRequest.PatientDetails details) {
        return patientRepository.findByUserId(user.getId())
                .orElseGet(() -> persistPatientWithConsent(newPatient(user.getId(), details)));
    }

    private Patient newPatient(Long userId, BookAppointmentRequest.PatientDetails details) {
        Patient patient = new Patient();
        patient.setUserId(userId);
        patient.setFullName(details.fullName());
        patient.setPhone(details.phone());
        patient.setDob(details.dob());
        patient.setGender(details.gender());
        patient.setCreatedAt(OffsetDateTime.now());
        return patient;
    }

    /** Persist a new patient and capture their consent (NFR-02) in the same step. */
    private Patient persistPatientWithConsent(Patient patient) {
        Patient saved = patientRepository.save(patient);
        consentRecordRepository.save(
                new ConsentRecord(saved.getId(), consentVersion, OffsetDateTime.now()));
        return saved;
    }

    private AppointmentResponse toResponse(
            Appointment appointment, Slot slot, Map<Long, String> serviceNames) {
        List<AppointmentResponse.StudyLine> lines = appointment.getStudies().stream()
                .map(study -> new AppointmentResponse.StudyLine(
                        study.getServiceId(),
                        serviceNames.get(study.getServiceId()),
                        study.getPriceSnapshot()))
                .toList();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                slot.getId(),
                slot.getStartTime(),
                appointment.getStatus().name(),
                appointment.getBilledAmount(),
                appointment.getReferringDoctorId(),
                lines);
    }

    /** Validate the (optional) chosen referring doctor exists and is active. */
    private Long resolveReferringDoctorId(Long referringDoctorId) {
        if (referringDoctorId == null) {
            return null;
        }
        return referringDoctorRepository.findByIdAndActiveTrue(referringDoctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown or inactive referring doctor"))
                .getId();
    }

    /**
     * Mark a BOOKED appointment COMPLETED (a staff/back-office action). On
     * completion, if the visit was referred, the referral engine computes and
     * records the doctor's payout (idempotent, so completing is safe to retry).
     */
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only a booked appointment can be completed (current status: "
                            + appointment.getStatus() + ")");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        Set<Long> serviceIds = appointment.getStudies().stream()
                .map(AppointmentStudy::getServiceId)
                .collect(Collectors.toSet());
        referralService.recordFor(
                appointment.getId(),
                appointment.getReferringDoctorId(),
                serviceIds,
                appointment.getBilledAmount());

        Slot slot = slotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        return toResponse(appointment, slot, loadServiceNames(List.of(appointment)));
    }
}
