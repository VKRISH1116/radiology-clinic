package com.clinic.audit.dto;

import com.clinic.audit.AuditLog;
import java.time.OffsetDateTime;

/** One audit-trail entry for the admin view. */
public record AuditLogResponse(
        Long id,
        Long userId,
        String action,
        String entity,
        Long entityId,
        OffsetDateTime createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntity(),
                log.getEntityId(),
                log.getCreatedAt());
    }
}
