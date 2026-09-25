package com.horizon.common.idempotency;

import com.horizon.common.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Deliberately not transactional: the duplicate-key failure must be caught <em>outside</em> the
 * transaction that produced it, otherwise the rolled-back transaction would poison the follow-up
 * read.
 */
@Service
class IdempotencyService {

    private static final int MAX_ATTEMPTS = 2;

    private final IdempotencyStore store;

    IdempotencyService(IdempotencyStore store) {
        this.store = store;
    }

    IdempotencyOutcome begin(UUID userId, String key, String requestHash) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Instant now = Instant.now();
            try {
                return new IdempotencyOutcome.Proceed(store.claim(userId, key, requestHash, now));
            } catch (DataIntegrityViolationException duplicateKey) {
                IdempotencyOutcome outcome = store.resolve(userId, key, requestHash, now);
                if (!(outcome instanceof IdempotencyOutcome.Retry)) {
                    return outcome;
                }
            }
        }
        throw ApiException.conflict("idempotency_in_flight", IdempotencyStore.IN_FLIGHT_MESSAGE);
    }

    void complete(UUID recordId, int responseStatus, String responseBody) {
        store.complete(recordId, responseStatus, responseBody);
    }

    void release(UUID recordId) {
        store.release(recordId);
    }
}
