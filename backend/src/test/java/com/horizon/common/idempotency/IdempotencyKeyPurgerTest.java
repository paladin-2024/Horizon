package com.horizon.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.horizon.common.purge.PurgeJob;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IdempotencyKeyPurgerTest {

    @Autowired
    IdempotencyKeyPurger purger;

    @Autowired
    PurgeJob purgeJob;

    @Autowired
    IdempotencyKeyRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void isNamedForTheTableItCleans() {
        assertThat(purger.name()).isEqualTo("idempotency-keys");
    }

    @Test
    void deletesOnlyExpiredRowsAndStopsAtTheBatchSize() {
        UUID scopeId = UUID.randomUUID();
        List<String> expired = insert(scopeId, 7, Instant.now().minusSeconds(60));
        List<String> live = insert(scopeId, 3, Instant.now().plusSeconds(3600));

        int firstBatch = purger.purgeBatch(4);
        int secondBatch = purger.purgeBatch(4);
        int thirdBatch = purger.purgeBatch(4);

        assertThat(firstBatch).isEqualTo(4);
        assertThat(secondBatch).isEqualTo(3);
        assertThat(thirdBatch).isZero();
        assertThat(remaining(scopeId, expired)).isZero();
        assertThat(remaining(scopeId, live)).isEqualTo(3);
    }

    @Test
    void thePurgeJobRunsIt() {
        UUID scopeId = UUID.randomUUID();
        List<String> expired = insert(scopeId, 5, Instant.now().minusSeconds(60));

        purgeJob.run();

        assertThat(remaining(scopeId, expired)).isZero();
    }

    @Test
    void aFreshKeyIsKeptForFortyEightHours() {
        UUID scopeId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                insert into idempotency_keys
                    (id, user_id, scope_id, key, request_hash, status, locked_until, created_at, expires_at)
                values (?, ?, ?, ?, ?, 'IN_PROGRESS', now(), now(), now() + interval '48 hours')
                """,
                UUID.randomUUID(), scopeId, scopeId, key, "c".repeat(64));

        purgeJob.run();

        assertThat(repository.findByScopeIdAndKey(scopeId, key)).isPresent();
    }

    private List<String> insert(UUID scopeId, int count, Instant expiresAt) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String key = UUID.randomUUID().toString();
            jdbcTemplate.update(
                    """
                    insert into idempotency_keys
                        (id, user_id, scope_id, key, request_hash, status, locked_until, created_at, expires_at)
                    values (?, ?, ?, ?, ?, 'COMPLETED', ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    scopeId,
                    scopeId,
                    key,
                    "d".repeat(64),
                    java.sql.Timestamp.from(Instant.now()),
                    java.sql.Timestamp.from(Instant.now()),
                    java.sql.Timestamp.from(expiresAt));
            keys.add(key);
        }
        return keys;
    }

    private long remaining(UUID scopeId, List<String> keys) {
        return keys.stream()
                .filter(key -> repository.findByScopeIdAndKey(scopeId, key).isPresent())
                .count();
    }
}
