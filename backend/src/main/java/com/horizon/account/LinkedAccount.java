package com.horizon.account;

import com.horizon.common.id.Uuid7;
import com.horizon.common.money.Money;
import com.horizon.linking.ProviderType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * One account a user linked at one institution, in exactly one currency.
 *
 * <p>{@code institutionId} is a plain column, not an association: the account module must not map the linking
 * module's entity. The database foreign key arrives with the first Flyway migration; until then
 * {@code LinkedAccountService.create} checks the institution exists.
 *
 * <p>The version is a {@link Long}, not a primitive: Spring Data decides a new entity by a null version, and the
 * id is assigned in the constructor, so a primitive version would turn every insert into a merge.
 */
@Entity
@Table(name = "linked_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_linked_accounts_user_institution_mask",
                columnNames = {"user_id", "institution_id", "account_mask"}),
        indexes = @Index(name = "idx_linked_accounts_user_id", columnList = "user_id"))
class LinkedAccount {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 16)
    private ProviderType provider;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "account_mask", nullable = false, length = 4)
    private String accountMask;

    @Embedded
    @AttributeOverride(name = "amountMinor", column = @Column(name = "current_balance_minor", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    private Money balance;

    @Column(name = "balance_as_of", nullable = false)
    private Instant balanceAsOf;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private LinkedAccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected LinkedAccount() {
    }

    LinkedAccount(UUID userId, UUID institutionId, ProviderType provider, String displayName,
            String accountMask, Money openingBalance, Instant now) {
        this.id = Uuid7.next();
        this.userId = userId;
        this.institutionId = institutionId;
        this.provider = provider;
        this.displayName = displayName;
        this.accountMask = accountMask;
        this.balance = openingBalance;
        this.balanceAsOf = now;
        this.status = LinkedAccountStatus.ACTIVE;
        this.createdAt = now;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getInstitutionId() {
        return institutionId;
    }

    ProviderType getProvider() {
        return provider;
    }

    String getDisplayName() {
        return displayName;
    }

    String getAccountMask() {
        return accountMask;
    }

    Money getBalance() {
        return balance;
    }

    Instant getBalanceAsOf() {
        return balanceAsOf;
    }

    LinkedAccountStatus getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Long getVersion() {
        return version;
    }

    /**
     * Adds {@code deltaMinor} (negative is money out) in the account's own currency and moves
     * {@code balanceAsOf} forward only if {@code asOf} is later than the balance we already have.
     */
    void applyDelta(long deltaMinor, Instant asOf) {
        this.balance = this.balance.plus(Money.of(deltaMinor, this.balance.currency()));
        if (asOf != null && asOf.isAfter(this.balanceAsOf)) {
            this.balanceAsOf = asOf;
        }
    }
}
