package com.horizon.common.ratelimit;

import java.time.Duration;

/** How many requests ({@code capacity}) are allowed per {@code refillPeriod}. */
public record RateLimitPolicy(long capacity, Duration refillPeriod) {

    public RateLimitPolicy {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("refillPeriod must be positive");
        }
    }
}
