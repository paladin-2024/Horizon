package com.horizon.common.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Single-instance rate limiter backed by in-memory Bucket4j buckets. The spec keeps buckets in
 * memory for v1 and moves them to Redis behind this same interface when we run more than one
 * instance.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private record Entry(Bucket bucket, long capacity) {}

    private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
    private final TimeMeter timeMeter;

    public InMemoryRateLimiter() {
        this(TimeMeter.SYSTEM_MILLISECONDS);
    }

    InMemoryRateLimiter(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    @Override
    public RateLimitResult tryConsume(String bucketKey, RateLimitPolicy policy) {
        Entry entry = buckets.computeIfAbsent(mapKey(bucketKey, policy), key -> newEntry(policy));
        ConsumptionProbe probe = entry.bucket().tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new RateLimitResult(true, Duration.ZERO);
        }
        return new RateLimitResult(false, Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    /** Drops buckets that are back at full capacity, so per-phone and per-IP keys cannot grow forever. */
    public int evictFullBuckets() {
        int before = buckets.size();
        buckets.entrySet().removeIf(entry -> entry.getValue().bucket().getAvailableTokens() >= entry.getValue().capacity());
        return before - buckets.size();
    }

    /** Visible for tests. */
    int bucketCount() {
        return buckets.size();
    }

    @Scheduled(fixedDelayString = "${horizon.rate-limit.sweep-interval-ms:300000}")
    void sweep() {
        evictFullBuckets();
    }

    private Entry newEntry(RateLimitPolicy policy) {
        Bucket bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(policy.capacity()).refillGreedy(policy.capacity(), policy.refillPeriod()))
                .withCustomTimePrecision(timeMeter)
                .build();
        return new Entry(bucket, policy.capacity());
    }

    private static String mapKey(String bucketKey, RateLimitPolicy policy) {
        return policy.capacity() + "/" + policy.refillPeriod().toMillis() + "|" + bucketKey;
    }
}
