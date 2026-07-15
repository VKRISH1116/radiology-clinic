package com.clinic.report;

import com.clinic.report.ReportService.ReportDownload;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Report upload/download for an appointment.
 *   POST (multipart) — STAFF/ADMIN only.
 *   GET             — authenticated; the service authorizes owner-or-staff and
 *                     404s otherwise (so patients can't probe others' reports).
 */
@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/api/appointments/{id}/report")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void upload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        reportService.upload(id, file, authentication.getName());
    }

    @GetMapping("/api/appointments/{id}/report")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id, Authentication authentication) {
        ReportDownload report = reportService.download(id, authentication);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + report.filename() + "\"")
                .body(report.content());
    }
}
