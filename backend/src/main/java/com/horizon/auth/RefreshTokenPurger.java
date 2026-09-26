package com.horizon.auth;

import com.horizon.common.purge.DataPurger;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes refresh tokens whose expiry has passed. Revoked rows stay until they expire: a rotated
 * token has to remain findable, or reuse detection would silently stop working.
 */
@Component
class RefreshTokenPurger implements DataPurger {

    private final RefreshTokenRepository repository;

    RefreshTokenPurger(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "refresh_tokens";
    }

    @Override
    @Transactional
    public int purgeBatch(int batchSize) {
        return repository.deleteExpiredBatch(Instant.now(), batchSize);
    }
}
