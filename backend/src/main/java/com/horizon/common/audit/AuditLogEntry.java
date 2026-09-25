package com.horizon.common.audit;

import com.horizon.common.id.Uuid7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One row of the {@code audit_log} table. Written only through {@link AuditLog}. */
@Entity
@Table(name = "audit_log", indexes = @Index(name = "idx_audit_log_user_created", columnList = "user_id, created_at"))
class AuditLogEntry {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(length = 45)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntry() {
    }

    AuditLogEntry(UUID userId, String action, Map<String, Object> metadata, String ip) {
        this.id = Uuid7.next();
        this.userId = userId;
        this.action = action;
        this.metadata = metadata;
        this.ip = ip;
        this.createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getAction() {
        return action;
    }

    Map<String, Object> getMetadata() {
        return metadata;
    }

    String getIp() {
        return ip;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
