package com.horizon.common.audit;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records sensitive actions (login, logout, account linking, imports). The row joins the caller's
 * transaction, so an action that rolls back leaves no audit row. The client IP is taken from the
 * current request when there is one.
 */
@Service
public class AuditLog {

    private final AuditLogRepository repository;

    AuditLog(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * @param userId   the acting user, or null for an unauthenticated action
     * @param action   a short dotted name such as {@code auth.login}
     * @param metadata extra JSON-serializable details; never put secrets in it; null is stored as {}
     */
    @Transactional
    public void record(UUID userId, String action, Map<String, Object> metadata) {
        repository.save(new AuditLogEntry(userId, action, metadata == null ? Map.of() : metadata, currentIp()));
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
