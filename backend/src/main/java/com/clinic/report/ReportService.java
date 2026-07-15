package com.clinic.report;

import com.clinic.audit.AuditService;
import com.clinic.booking.Appointment;
import com.clinic.booking.AppointmentRepository;
import com.clinic.patient.Patient;
import com.clinic.patient.PatientRepository;
import com.clinic.user.User;
import com.clinic.user.UserRepository;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Uploading and downloading appointment reports. Uploads are validated as real
 * PDFs (content type + magic bytes) and size-capped; downloads are authorized to
 * the owning patient or any staff/admin. Both actions are audit-logged.
 */
@Service
public class ReportService {

    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private final ReportRepository reportRepository;
    private final ReportStorage reportStorage;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ReportService(
            ReportRepository reportRepository,
            ReportStorage reportStorage,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.reportRepository = reportRepository;
        this.reportStorage = reportStorage;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** Store (or replace) the report PDF for an appointment. Staff/admin only. */
    @Transactional
    public void upload(Long appointmentId, MultipartFile file, String actorEmail) {
        appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        byte[] bytes = readAndValidate(file);
        String storedName = reportStorage.store(bytes);

        Report report = reportRepository.findByAppointmentId(appointmentId).orElseGet(Report::new);
        report.setAppointmentId(appointmentId);
        report.setFilePath(storedName);
        report.setUploadedBy(userId(actorEmail));
        report.setUploadedAt(OffsetDateTime.now());
        reportRepository.save(report);

        auditService.record("REPORT_UPLOAD", "appointment", appointmentId, actorEmail);
    }

    /** Fetch the report PDF bytes if the caller is allowed to see them. */
    @Transactional
    public ReportDownload download(Long appointmentId, Authentication authentication) {
        Report report = reportRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Report not found"));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Report not found"));

        authorize(appointment, authentication);
        byte[] bytes = reportStorage.read(report.getFilePath());
        auditService.record("REPORT_DOWNLOAD", "appointment", appointmentId, authentication.getName());
        return new ReportDownload(bytes, "report-" + appointmentId + ".pdf");
    }

    /** Staff/admin may see any report; a patient may see only their own. */
    private void authorize(Appointment appointment, Authentication authentication) {
        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        if (staff) {
            return;
        }
        // Otherwise the caller must be the patient the appointment belongs to.
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        Long patientId = patientRepository.findByUserId(user.getId())
                .map(Patient::getId)
                .orElse(null);
        if (patientId == null || !patientId.equals(appointment.getPatientId())) {
            // 404, not 403, so a patient can't probe which appointment ids exist.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
        }
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large (max 20MB)");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
        // Don't trust the declared content type alone — check the %PDF magic bytes.
        if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a valid PDF");
        }
        return bytes;
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    /** The bytes of a report plus a suggested download filename. */
    public record ReportDownload(byte[] content, String filename) {
    }
}
