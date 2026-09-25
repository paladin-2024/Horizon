package com.horizon.common.idempotency;

import com.horizon.common.id.Uuid7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per {@code (scope, key)}. The unique constraint on {@code (scope_id, key)} is what makes
 * concurrent requests with the same key safe: exactly one insert can win.
 *
 * <p>{@code user_id} stays nullable (unauthenticated endpoints such as register and login), but the
 * constraint uses {@code scope_id}, because PostgreSQL treats NULLs in a unique constraint as
 * distinct and would let two anonymous requests with the same key both insert.
 */
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_keys_scope_key", columnNames = {"scope_id", "key"}),
        indexes = @Index(name = "idx_idempotency_keys_expires_at", columnList = "expires_at"))
public class IdempotencyKey {

    /** Scope for unauthenticated requests. */
    public static final UUID ANONYMOUS_SCOPE = new UUID(0L, 0L);

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Column(name = "key", nullable = false, length = 200)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    // Plain text, not @JdbcTypeCode(SqlTypes.JSON): the JSON type mapper re-serializes the string
    // (e.g. `{"id":"1"}` comes back as `{"id": "1"}`), which breaks the "replays byte-for-byte"
    // contract. The stored value is a caller-supplied response body already serialized upstream.
    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "locked_until", nullable = false)
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyKey() {}

    IdempotencyKey(UUID userId, String key, String requestHash, Instant now, Instant lockedUntil, Instant expiresAt) {
        this.id = Uuid7.next();
        this.userId = userId;
        this.scopeId = userId != null ? userId : ANONYMOUS_SCOPE;
        this.key = key;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.createdAt = now;
        this.lockedUntil = lockedUntil;
        this.expiresAt = expiresAt;
    }

    void complete(int responseStatus, String responseBody) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public String getKey() {
        return key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
