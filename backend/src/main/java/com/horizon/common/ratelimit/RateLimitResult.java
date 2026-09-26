package com.horizon.common.ratelimit;

import java.time.Duration;

/** The outcome of one token request. {@code retryAfter} is {@link Duration#ZERO} when allowed. */
public record RateLimitResult(boolean allowed, Duration retryAfter) {

    public RateLimitResult {
        if (retryAfter == null) {
            throw new IllegalArgumentException("retryAfter must not be null");
        }
    }

    /** Whole seconds for the {@code Retry-After} header: always at least 1 when rejected. */
    public long retryAfterSeconds() {
        if (allowed) {
            return 0L;
        }
        long seconds = retryAfter.plusNanos(999_999_999L).toSeconds();
        return Math.max(1L, seconds);
    }
}
