package com.horizon.common.idempotency;

import com.horizon.common.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every method runs in its own committed transaction: the claim has to be visible to concurrent
 * requests before the handler runs, and completion happens after the handler's own transaction has
 * already committed.
 */
@Component
class IdempotencyStore {

    static final String IN_FLIGHT_MESSAGE = "A request with this Idempotency-Key is still in progress.";
    static final String REUSED_MESSAGE = "This Idempotency-Key was already used with a different request.";

    private final IdempotencyKeyRepository repository;
    private final IdempotencyProperties properties;

    IdempotencyStore(IdempotencyKeyRepository repository, IdempotencyProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Inserts the in-flight row. Throws {@link org.springframework.dao.DataIntegrityViolationException}
     * when the key is taken — that exception must be caught outside this transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID claim(UUID userId, String key, String requestHash, Instant now) {
        IdempotencyKey record = new IdempotencyKey(
                userId, key, requestHash, now, now.plus(properties.lockTimeout()), now.plus(properties.retention()));
        repository.saveAndFlush(record);
        return record.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyOutcome resolve(UUID userId, String key, String requestHash, Instant now) {
        UUID scopeId = userId != null ? userId : IdempotencyKey.ANONYMOUS_SCOPE;
        IdempotencyKey record = repository.findByScopeIdAndKey(scopeId, key).orElse(null);
        if (record == null) {
            return new IdempotencyOutcome.Retry();
        }
        if (!record.getRequestHash().equals(requestHash)) {
            throw ApiException.unprocessable("idempotency_key_reused", REUSED_MESSAGE);
        }
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return new IdempotencyOutcome.Replay(record.getResponseStatus(), record.getResponseBody());
        }
        if (record.getLockedUntil().isAfter(now)) {
            throw ApiException.conflict("idempotency_in_flight", IN_FLIGHT_MESSAGE);
        }
        int takenOver = repository.takeOverStaleLock(
                record.getId(), IdempotencyStatus.IN_PROGRESS, now, now.plus(properties.lockTimeout()));
        if (takenOver == 1) {
            return new IdempotencyOutcome.Proceed(record.getId());
        }
        throw ApiException.conflict("idempotency_in_flight", IN_FLIGHT_MESSAGE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID recordId, int responseStatus, String responseBody) {
        repository.findById(recordId).ifPresent(record -> record.complete(responseStatus, responseBody));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(UUID recordId) {
        repository.findById(recordId).ifPresent(repository::delete);
    }
}
