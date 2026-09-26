package com.horizon.auth;

import com.horizon.common.audit.AuditLog;
import com.horizon.common.error.ApiException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Refresh tokens live in families. Every use rotates the token inside its family; presenting a token
 * that was already rotated away means it leaked, so the whole family is revoked.
 *
 * <p>Deliberately not {@code @Transactional} itself: {@link RefreshTokenStore#rotate} must commit
 * the revocation of a reused token before this class throws, and a method calling another method
 * on {@code this} never goes through the {@code @Transactional} proxy, so that store lives in its
 * own bean (same split as plan 02's {@code IdempotencyService}/{@code IdempotencyStore}).
 */
@Service
class RefreshTokenService {

    record Rotation(UUID userId, String token) {
    }

    private final RefreshTokenStore store;
    private final AuditLog auditLog;

    RefreshTokenService(RefreshTokenStore store, AuditLog auditLog) {
        this.store = store;
        this.auditLog = auditLog;
    }

    /** Starts a new family (one per sign-in) and returns the token to send to the client. */
    String startFamily(UUID userId) {
        return store.startFamily(userId);
    }

    Rotation rotate(String presentedToken) {
        RefreshTokenStore.Outcome outcome = store.rotate(presentedToken);
        return switch (outcome.status()) {
            case ROTATED -> new Rotation(outcome.userId(), outcome.token());
            case REUSED -> {
                auditLog.record(outcome.userId(), "auth.refresh.reuse_detected", Map.of());
                throw ApiException.unauthorized("refresh_token_reused",
                        "This session was ended because of refresh token reuse. Sign in again.");
            }
            case EXPIRED, NOT_FOUND -> throw ApiException.unauthorized("invalid_refresh_token",
                    "Your session has expired. Sign in again.");
        };
    }

    /** Revokes every token in the presented token's family. Unknown tokens are ignored. */
    Optional<UUID> revokeFamilyOf(String presentedToken) {
        return store.revokeFamilyOf(presentedToken);
    }
}
