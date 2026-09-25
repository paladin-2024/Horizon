package com.horizon.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    /** Deterministic clock: Bucket4j only ever asks for nanos. */
    static final class FakeTimeMeter implements TimeMeter {
        private long nanos = 1_000_000_000_000L;

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }

    private final FakeTimeMeter clock = new FakeTimeMeter();
    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);
    private final RateLimitPolicy threePerMinute = new RateLimitPolicy(3, Duration.ofMinutes(1));

    @Test
    void allowsUpToCapacityThenRejects() {
        assertThat(limiter.tryConsume("phone:+256700000001", threePerMinute).allowed()).isTrue();
        assertThat(limiter.tryConsume("phone:+256700000001", threePerMinute).allowed()).isTrue();
        assertThat(limiter.tryConsume("phone:+256700000001", threePerMinute).allowed()).isTrue();

        RateLimitResult rejected = limiter.tryConsume("phone:+256700000001", threePerMinute);

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(20));
        assertThat(rejected.retryAfterSeconds()).isBetween(19L, 20L);
    }

    @Test
    void allowedResultHasNoRetryDelay() {
        RateLimitResult allowed = limiter.tryConsume("phone:+256700000002", threePerMinute);

        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.retryAfter()).isZero();
        assertThat(allowed.retryAfterSeconds()).isZero();
    }

    @Test
    void refillsOverTime() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("phone:+256700000003", threePerMinute);
        }
        assertThat(limiter.tryConsume("phone:+256700000003", threePerMinute).allowed()).isFalse();

        clock.advance(Duration.ofSeconds(20));

        assertThat(limiter.tryConsume("phone:+256700000003", threePerMinute).allowed()).isTrue();
        assertThat(limiter.tryConsume("phone:+256700000003", threePerMinute).allowed()).isFalse();
    }

    @Test
    void bucketsAreIndependentPerKeyAndPerPolicy() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("a", threePerMinute);
        }

        assertThat(limiter.tryConsume("a", threePerMinute).allowed()).isFalse();
        assertThat(limiter.tryConsume("b", threePerMinute).allowed()).isTrue();
        assertThat(limiter.tryConsume("a", new RateLimitPolicy(1, Duration.ofMinutes(1))).allowed()).isTrue();
    }

    @Test
    void evictsOnlyBucketsThatAreFullAgain() {
        limiter.tryConsume("idle", threePerMinute);
        limiter.tryConsume("busy", threePerMinute);
        limiter.tryConsume("busy", threePerMinute);
        limiter.tryConsume("busy", threePerMinute);

        clock.advance(Duration.ofSeconds(20)); // "idle" is back to 3 tokens, "busy" only has 1

        int evicted = limiter.evictFullBuckets();

        assertThat(evicted).isEqualTo(1);
        assertThat(limiter.bucketCount()).isEqualTo(1);
    }

    @Test
    void policyRejectsNonPositiveValues() {
        assertThat(RateLimitPolicies.OTP_SEND).isEqualTo(new RateLimitPolicy(3, Duration.ofMinutes(10)));
        assertThat(RateLimitPolicies.OTP_SEND_PER_IP).isEqualTo(new RateLimitPolicy(10, Duration.ofHours(1)));
        assertThat(RateLimitPolicies.LOGIN_PER_IP).isEqualTo(new RateLimitPolicy(5, Duration.ofMinutes(1)));
        assertThat(RateLimitPolicies.LOGIN_PER_ACCOUNT).isEqualTo(new RateLimitPolicy(5, Duration.ofMinutes(1)));
        assertThat(RateLimitPolicies.GENERAL_PER_USER).isEqualTo(new RateLimitPolicy(100, Duration.ofMinutes(1)));
    }
}
