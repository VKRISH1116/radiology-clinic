package com.clinic.audit;

import com.clinic.audit.dto.AuditLogResponse;
import com.clinic.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads the audit trail. Called explicitly at the few sensitive points
 * (report download/upload, payout update). An AOP aspect could generalise this,
 * but explicit calls keep it obvious which actions are recorded and why.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Record an action. Resolves the acting user's id from their email (nullable —
     * a missing/unknown actor is still logged as an anonymous action).
     */
    @Transactional
    public void record(String action, String entity, Long entityId, String actorEmail) {
        Long userId = actorEmail == null ? null
                : userRepository.findByEmail(actorEmail).map(user -> user.getId()).orElse(null);
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setCreatedAt(OffsetDateTime.now());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> recent() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
