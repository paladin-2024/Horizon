package com.horizon.common.purge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PurgeJobTest.CountingPurgerConfig.class)
class PurgeJobTest {

    /** Reports 1200 rows to delete, so the job has to run three batches of 500. */
    static class CountingPurger implements DataPurger {

        private final AtomicInteger remaining = new AtomicInteger(1200);
        private final List<Integer> batches = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public String name() {
            return "counting";
        }

        @Override
        public int purgeBatch(int batchSize) {
            int deleted = Math.min(batchSize, Math.max(0, remaining.get()));
            remaining.addAndGet(-deleted);
            batches.add(deleted);
            return deleted;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CountingPurgerConfig {

        @Bean
        CountingPurger countingPurger() {
            return new CountingPurger();
        }
    }

    @Autowired
    PurgeJob purgeJob;

    @Autowired
    CountingPurger countingPurger;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void deletesInBatchesUntilABatchComesBackShortAndHoldsTheLock() {
        purgeJob.run();

        assertThat(countingPurger.batches).containsExactly(500, 500, 200);
        Integer locks = jdbcTemplate.queryForObject(
                "select count(*) from shedlock where name = 'purgeJob'", Integer.class);
        assertThat(locks).isEqualTo(1);
    }
}
