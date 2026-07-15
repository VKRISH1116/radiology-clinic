package com.clinic.booking;

import com.clinic.booking.dto.AppointmentResponse;
import com.clinic.booking.dto.BookAppointmentRequest;
import com.clinic.booking.dto.RescheduleRequest;
import com.clinic.booking.dto.SlotAvailabilityResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The booking flow, for any authenticated user:
 *   GET  /api/slots?date=YYYY-MM-DD  -> the day's slots with availability
 *   POST /api/appointments           -> book a slot for one or more studies
 *
 * The current user's identity comes from the JWT (Authentication.getName() is the
 * email), so a patient can only ever book for themselves.
 */
@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/slots")
    public List<SlotAvailabilityResponse> slots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.listAvailableSlots(date);
    }

    @PostMapping("/api/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(
            @Valid @RequestBody BookAppointmentRequest request,
            Authentication authentication) {
        return bookingService.book(request, authentication.getName());
    }

    /**
     * Staff/back-office booking for a walk-in patient (no login account). Same body
     * as a normal booking; the patient details create a fresh unlinked patient.
     */
    @PostMapping("/api/appointments/walk-in")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse bookWalkIn(
            @Valid @RequestBody BookAppointmentRequest request,
            Authentication authentication) {
        return bookingService.bookWalkIn(request, authentication.getName());
    }

    /** The caller's own appointments, newest first. */
    @GetMapping("/api/appointments/mine")
    public List<AppointmentResponse> myAppointments(Authentication authentication) {
        return bookingService.listMyAppointments(authentication.getName());
    }

    /**
     * Cancel one of the caller's own appointments. POST (not DELETE) because this
     * is a status transition to CANCELLED, not a deletion of the record.
     */
    @PostMapping("/api/appointments/{id}/cancel")
    public AppointmentResponse cancel(
            @PathVariable Long id, Authentication authentication) {
        return bookingService.cancelMyAppointment(id, authentication.getName());
    }

    /** Move one of the caller's own BOOKED appointments to a different slot. */
    @PostMapping("/api/appointments/{id}/reschedule")
    public AppointmentResponse reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request,
            Authentication authentication) {
        return bookingService.rescheduleMyAppointment(id, request.slotId(), authentication.getName());
    }

    /**
     * Mark an appointment completed — a staff/back-office action (patients can't
     * complete their own visit). Triggers the referral payout calculation.
     */
    @PostMapping("/api/appointments/{id}/complete")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public AppointmentResponse complete(@PathVariable Long id) {
        return bookingService.completeAppointment(id);
    }
}
