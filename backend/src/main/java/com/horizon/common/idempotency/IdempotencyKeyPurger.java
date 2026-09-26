package com.horizon.common.idempotency;

import com.horizon.common.purge.DataPurger;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Deletes idempotency keys past their 48 hour retention, in batches. */
@Component
public class IdempotencyKeyPurger implements DataPurger {

    private final IdempotencyKeyRepository repository;

    IdempotencyKeyPurger(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "idempotency-keys";
    }

    @Override
    @Transactional
    public int purgeBatch(int batchSize) {
        return repository.deleteExpiredBatch(Instant.now(), batchSize);
    }
}
