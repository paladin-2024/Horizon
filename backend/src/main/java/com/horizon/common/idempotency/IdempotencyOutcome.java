package com.horizon.common.idempotency;

import java.util.UUID;

/** What the caller should do with a request that carries an Idempotency-Key. */
sealed interface IdempotencyOutcome {

    /** This request owns the key: run the handler, then complete or release {@code recordId}. */
    record Proceed(UUID recordId) implements IdempotencyOutcome {}

    /** The key already carries a finished response: return it unchanged. */
    record Replay(int status, String body) implements IdempotencyOutcome {}

    /** The row vanished between the failed insert and the read (purged): try the insert once more. */
    record Retry() implements IdempotencyOutcome {}
}
