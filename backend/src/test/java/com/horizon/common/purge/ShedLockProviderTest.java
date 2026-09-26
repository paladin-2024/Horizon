package com.horizon.common.purge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ShedLockProviderTest {

    @Autowired
    LockProvider lockProvider;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void theShedlockTableExists() {
        Integer tables = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'shedlock'", Integer.class);

        assertThat(tables).isEqualTo(1);
    }

    @Test
    void onlyOneHolderCanTakeTheSameLock() {
        LockConfiguration configuration = new LockConfiguration(
                Instant.now(), "test-" + UUID.randomUUID(), Duration.ofMinutes(5), Duration.ZERO);

        Optional<SimpleLock> first = lockProvider.lock(configuration);
        Optional<SimpleLock> second = lockProvider.lock(configuration);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();

        first.orElseThrow().unlock();

        assertThat(lockProvider.lock(configuration)).isPresent();
    }
}
