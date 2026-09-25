package com.horizon.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class MoneyPersistenceTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void embeddedMoneyIsStoredInTheOverriddenColumns() {
        MoneyProbe probe = new MoneyProbe(Money.of(-12_345, "CDF"));
        transactions.executeWithoutResult(status -> entityManager.persist(probe));

        Map<String, Object> row = jdbc.queryForMap(
                "select balance_minor, balance_currency from money_probe where id = ?", probe.getId());

        assertThat(((Number) row.get("balance_minor")).longValue()).isEqualTo(-12_345L);
        assertThat(row.get("balance_currency")).isEqualTo("CDF");
    }

    @Test
    void moneyReadsBackAsEqualValue() {
        MoneyProbe probe = new MoneyProbe(Money.of(5_000, "UGX"));
        transactions.executeWithoutResult(status -> entityManager.persist(probe));
        UUID id = probe.getId();

        Money loaded = transactions.execute(status -> entityManager.find(MoneyProbe.class, id).getBalance());

        assertThat(loaded).isEqualTo(Money.of(5_000, "UGX"));
    }
}
