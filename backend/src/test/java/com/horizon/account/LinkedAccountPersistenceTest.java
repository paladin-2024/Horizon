package com.horizon.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.horizon.common.money.Money;
import com.horizon.linking.ProviderType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
@ActiveProfiles("test")
class LinkedAccountPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Autowired
    private LinkedAccountRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private LinkedAccount newAccount(UUID userId, UUID institutionId, String mask, Money balance) {
        return new LinkedAccount(userId, institutionId, ProviderType.MANUAL, "Salary account", mask, balance, NOW);
    }

    @Test
    void mapsMoneyIntoTheOverriddenColumns() {
        UUID userId = UUID.randomUUID();
        LinkedAccount saved = repository.saveAndFlush(
                newAccount(userId, UUID.randomUUID(), "4821", Money.of(250_000L, "UGX")));

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Object[] row = tx.execute(status -> (Object[]) entityManager.createNativeQuery(
                        "select current_balance_minor, currency, version, status from linked_accounts where id = ?1")
                .setParameter(1, saved.getId())
                .getSingleResult());

        assertThat(((Number) row[0]).longValue()).isEqualTo(250_000L);
        assertThat(row[1]).isEqualTo("UGX");
        assertThat(((Number) row[2]).longValue()).isZero();
        assertThat(row[3]).isEqualTo("ACTIVE");
    }

    @Test
    void readsBackTheWholeEntity() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID id = repository.saveAndFlush(newAccount(userId, institutionId, "0099", Money.of(-1_500L, "CDF"))).getId();

        LinkedAccount found = repository.findById(id).orElseThrow();

        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getInstitutionId()).isEqualTo(institutionId);
        assertThat(found.getProvider()).isEqualTo(ProviderType.MANUAL);
        assertThat(found.getDisplayName()).isEqualTo("Salary account");
        assertThat(found.getAccountMask()).isEqualTo("0099");
        assertThat(found.getBalance()).isEqualTo(Money.of(-1_500L, "CDF"));
        assertThat(found.getBalanceAsOf()).isEqualTo(NOW);
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
        assertThat(found.getStatus()).isEqualTo(LinkedAccountStatus.ACTIVE);
        assertThat(found.getVersion()).isZero();
    }

    @Test
    void rejectsTheSameInstitutionAndMaskTwiceForOneUser() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        repository.saveAndFlush(newAccount(userId, institutionId, "4821", Money.of(1L, "USD")));

        assertThatThrownBy(() -> repository.saveAndFlush(newAccount(userId, institutionId, "4821", Money.of(2L, "USD"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsTheSameInstitutionAndMaskForAnotherUser() {
        UUID institutionId = UUID.randomUUID();
        repository.saveAndFlush(newAccount(UUID.randomUUID(), institutionId, "4821", Money.of(1L, "USD")));
        repository.saveAndFlush(newAccount(UUID.randomUUID(), institutionId, "4821", Money.of(2L, "USD")));

        assertThat(repository.count()).isPositive();
    }

    @Test
    void applyDeltaAddsToTheBalanceAndMovesBalanceAsOfForward() {
        LinkedAccount account = newAccount(UUID.randomUUID(), UUID.randomUUID(), "1234", Money.of(1_000L, "UGX"));

        account.applyDelta(-250L, Instant.parse("2026-02-01T00:00:00Z"));

        assertThat(account.getBalance()).isEqualTo(Money.of(750L, "UGX"));
        assertThat(account.getBalanceAsOf()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    void applyDeltaKeepsTheLaterBalanceAsOf() {
        LinkedAccount account = newAccount(UUID.randomUUID(), UUID.randomUUID(), "1234", Money.of(1_000L, "UGX"));

        account.applyDelta(250L, Instant.parse("2025-12-01T00:00:00Z"));

        assertThat(account.getBalance()).isEqualTo(Money.of(1_250L, "UGX"));
        assertThat(account.getBalanceAsOf()).isEqualTo(NOW);
    }
}
