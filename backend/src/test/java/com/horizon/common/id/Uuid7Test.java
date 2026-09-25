package com.horizon.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class Uuid7Test {

    @Test
    void hasVersion7AndRfcVariant() {
        UUID id = Uuid7.next();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void embedsTheCurrentUnixMillisecondTimestamp() {
        long before = System.currentTimeMillis();
        UUID id = Uuid7.next();
        long after = System.currentTimeMillis();

        long timestamp = id.getMostSignificantBits() >>> 16;

        assertThat(timestamp).isBetween(before, after + 1);
    }

    @Test
    void idsFromOneThreadIncreaseStrictly() {
        String previous = Uuid7.next().toString();
        for (int i = 0; i < 20_000; i++) {
            String current = Uuid7.next().toString();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    @Test
    void idsAreUniqueAcrossThreads() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<List<UUID>>> futures = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                Callable<List<UUID>> task = () -> {
                    List<UUID> ids = new ArrayList<>();
                    for (int i = 0; i < 5_000; i++) {
                        ids.add(Uuid7.next());
                    }
                    return ids;
                };
                futures.add(pool.submit(task));
            }
            Set<UUID> all = new HashSet<>();
            for (Future<List<UUID>> future : futures) {
                all.addAll(future.get());
            }
            assertThat(all).hasSize(8 * 5_000);
        } finally {
            pool.shutdownNow();
        }
    }
}
