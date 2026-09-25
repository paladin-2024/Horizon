package com.horizon.common.money;

import com.horizon.common.id.Uuid7;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Test-only entity that shows how real entities embed Money with renamed columns. */
@Entity
@Table(name = "money_probe")
class MoneyProbe {

    @Id
    private UUID id;

    @Embedded
    @AttributeOverride(name = "amountMinor", column = @Column(name = "balance_minor", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "balance_currency", length = 3, nullable = false))
    private Money balance;

    protected MoneyProbe() {
    }

    MoneyProbe(Money balance) {
        this.id = Uuid7.next();
        this.balance = balance;
    }

    UUID getId() {
        return id;
    }

    Money getBalance() {
        return balance;
    }
}
