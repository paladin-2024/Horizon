package com.horizon.common.ratelimit;

import org.springframework.stereotype.Component;

/** Call site helper: consume one token or fail the request with 429. */
@Component
public class RateLimitGuard {

    private final RateLimiter rateLimiter;

    public RateLimitGuard(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public void check(String bucketKey, RateLimitPolicy policy) {
        RateLimitResult result = rateLimiter.tryConsume(bucketKey, policy);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfter());
        }
    }
}
