package com.horizon.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.horizon.common.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class IdempotencyServiceTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @Autowired
    IdempotencyService service;

    @Autowired
    IdempotencyKeyRepository repository;

    @Autowired
    IdempotencyProperties properties;

    @Autowired
    TransactionTemplate transactions;

    private String newKey() {
        return UUID.randomUUID().toString();
    }

    @Test
    void firstCallProceedsAndStoresAnInProgressRow() {
        String key = newKey();
        UUID userId = UUID.randomUUID();

        IdempotencyOutcome outcome = service.begin(userId, key, HASH_A);

        assertThat(outcome).isInstanceOf(IdempotencyOutcome.Proceed.class);
        IdempotencyKey stored = repository.findByScopeIdAndKey(userId, key).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
        assertThat(stored.getUserId()).isEqualTo(userId);
        assertThat(stored.getScopeId()).isEqualTo(userId);
        assertThat(stored.getLockedUntil()).isAfter(Instant.now());
        assertThat(stored.getExpiresAt()).isAfter(Instant.now().plusSeconds(47 * 3600));
    }

    @Test
    void anonymousRequestsShareTheZeroScopeAndStillCollide() {
        String key = newKey();

        assertThat(service.begin(null, key, HASH_A)).isInstanceOf(IdempotencyOutcome.Proceed.class);

        assertThatThrownBy(() -> service.begin(null, key, HASH_A))
                .isInstanceOf(ApiException.class)
                .hasMessage("A request with this Idempotency-Key is still in progress.");
        assertThat(repository.findByScopeIdAndKey(IdempotencyKey.ANONYMOUS_SCOPE, key)).isPresent();
    }

    @Test
    void theSameKeyForDifferentUsersDoesNotCollide() {
        String key = newKey();

        assertThat(service.begin(UUID.randomUUID(), key, HASH_A)).isInstanceOf(IdempotencyOutcome.Proceed.class);
        assertThat(service.begin(UUID.randomUUID(), key, HASH_A)).isInstanceOf(IdempotencyOutcome.Proceed.class);
    }

    @Test
    void aCompletedKeyReplaysTheStoredResponse() {
        String key = newKey();
        UUID userId = UUID.randomUUID();
        UUID recordId = ((IdempotencyOutcome.Proceed) service.begin(userId, key, HASH_A)).recordId();
        service.complete(recordId, 201, "{\"id\":\"1\"}");

        IdempotencyOutcome outcome = service.begin(userId, key, HASH_A);

        assertThat(outcome).isEqualTo(new IdempotencyOutcome.Replay(201, "{\"id\":\"1\"}"));
    }

    @Test
    void aDifferentBodyWithTheSameKeyIsRejected() {
        String key = newKey();
        UUID userId = UUID.randomUUID();
        service.begin(userId, key, HASH_A);

        assertThatThrownBy(() -> service.begin(userId, key, HASH_B))
                .isInstanceOf(ApiException.class)
                .hasMessage("This Idempotency-Key was already used with a different request.");
    }

    @Test
    void aReleasedKeyCanBeUsedAgain() {
        String key = newKey();
        UUID userId = UUID.randomUUID();
        UUID recordId = ((IdempotencyOutcome.Proceed) service.begin(userId, key, HASH_A)).recordId();

        service.release(recordId);

        assertThat(repository.findByScopeIdAndKey(userId, key)).isEmpty();
        assertThat(service.begin(userId, key, HASH_B)).isInstanceOf(IdempotencyOutcome.Proceed.class);
    }

    @Test
    void aStaleInFlightLockIsTakenOverInsteadOfBlockingForever() {
        String key = newKey();
        UUID userId = UUID.randomUUID();
        UUID recordId = ((IdempotencyOutcome.Proceed) service.begin(userId, key, HASH_A)).recordId();
        expireLock(userId, key);

        IdempotencyOutcome outcome = service.begin(userId, key, HASH_A);

        assertThat(outcome).isEqualTo(new IdempotencyOutcome.Proceed(recordId));
        assertThat(repository.findByScopeIdAndKey(userId, key).orElseThrow().getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void twoConcurrentRequestsWithTheSameKeyLeaveExactlyOneWinner() throws Exception {
        String key = newKey();
        UUID userId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Object> call = () -> {
            start.await();
            try {
                return service.begin(userId, key, HASH_A);
            } catch (RuntimeException failure) {
                return failure;
            }
        };

        Future<Object> first = pool.submit(call);
        Future<Object> second = pool.submit(call);
        start.countDown();
        List<Object> outcomes = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(outcomes).filteredOn(IdempotencyOutcome.Proceed.class::isInstance).hasSize(1);
        assertThat(outcomes)
                .filteredOn(ApiException.class::isInstance)
                .hasSize(1)
                .allSatisfy(failure -> assertThat((Throwable) failure)
                        .hasMessage("A request with this Idempotency-Key is still in progress."));
        assertThat(repository.findByScopeIdAndKey(userId, key)).isPresent();
    }

    private void expireLock(UUID scopeId, String key) {
        // takeOverStaleLock is a @Modifying query; it needs an active transaction to run, which
        // the production code always provides (IdempotencyStore.resolve is @Transactional). This
        // test helper calls the repository directly, so it opens its own transaction here.
        transactions.executeWithoutResult(status -> {
            IdempotencyKey record = repository.findByScopeIdAndKey(scopeId, key).orElseThrow();
            repository.takeOverStaleLock(
                    record.getId(),
                    IdempotencyStatus.IN_PROGRESS,
                    Instant.now().plusSeconds(3600),
                    Instant.now().minus(properties.lockTimeout()));
        });
    }
}
