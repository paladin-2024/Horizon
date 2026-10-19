package com.horizon.account;

import static com.horizon.account.AccountTestFixtures.draft;
import static com.horizon.account.AccountTestFixtures.seededInstitutionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.horizon.common.error.ApiException;
import com.horizon.linking.InstitutionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class BalanceDeltaConcurrencyTest {

    // Computed relative to the clock, not a fixed date: accountService.create() stamps balanceAsOf
    // with Instant.now() when each test creates its account, so a hardcoded past date would stop
    // being "later" once real time passed it (applyDelta never moves balanceAsOf backward).
    private static final Instant LATER = Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS);

    @Autowired
    private LinkedAccountService accountService;

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID newAccount(UUID userId, String mask, long openingMinor) {
        UUID institutionId = seededInstitutionId(institutionService, "UG", "stanbic-ug");
        return accountService.create(userId, draft(institutionId, mask, openingMinor, "UGX")).id();
    }

    @Test
    void refusesToRunWithoutACallerTransaction() {
        assertThatThrownBy(() -> accountService.applyBalanceDelta(UUID.randomUUID(), 100L, LATER))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void appliesDeltasInsideTheCallersTransaction() {
        UUID userId = UUID.randomUUID();
        UUID accountId = newAccount(userId, "1001", 250_000L);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            accountService.applyBalanceDelta(accountId, -50_000L, LATER);
            accountService.applyBalanceDelta(accountId, 10_000L, LATER);
        });

        LinkedAccountView view = accountService.get(userId, accountId);
        assertThat(view.currentBalanceMinor()).isEqualTo(210_000L);
        assertThat(view.balanceAsOf()).isEqualTo(LATER);
    }

    @Test
    void rollsBackWithTheCallersTransaction() {
        UUID userId = UUID.randomUUID();
        UUID accountId = newAccount(userId, "1002", 250_000L);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            accountService.applyBalanceDelta(accountId, -50_000L, LATER);
            throw new IllegalStateException("import failed after the delta");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(accountService.get(userId, accountId).currentBalanceMinor()).isEqualTo(250_000L);
    }

    @Test
    void reportsAMissingAccount() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status ->
                accountService.applyBalanceDelta(UUID.randomUUID(), 100L, LATER)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("account_not_found"));
    }

    @Test
    void twoConcurrentTransactionsCannotBothWinTheOptimisticLock() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = newAccount(userId, "1003", 5_000L);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CyclicBarrier bothHaveReadTheRow = new CyclicBarrier(2);

        Callable<Throwable> applyDelta = () -> {
            try {
                tx.executeWithoutResult(status -> {
                    // Loads the row at version 0 and mutates it; the UPDATE happens at commit.
                    accountService.applyBalanceDelta(accountId, 1_000L, LATER);
                    awaitQuietly(bothHaveReadTheRow);
                });
                return null;
            } catch (Throwable failure) {
                return failure;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Throwable>> futures;
        try {
            futures = pool.invokeAll(List.of(applyDelta, applyDelta));
        } finally {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        List<Throwable> failures = new ArrayList<>();
        for (Future<Throwable> future : futures) {
            Throwable failure = future.get();
            if (failure != null) {
                failures.add(failure);
            }
        }

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        // Exactly one delta was applied: the loser's transaction rolled back.
        assertThat(accountService.get(userId, accountId).currentBalanceMinor()).isEqualTo(6_000L);
    }

    private static void awaitQuietly(CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("barrier failed", e);
        }
    }
}
