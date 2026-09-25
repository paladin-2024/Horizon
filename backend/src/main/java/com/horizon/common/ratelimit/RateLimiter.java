package com.horizon.common.ratelimit;

/** Consumes one token from the bucket identified by {@code bucketKey} under {@code policy}. */
public interface RateLimiter {

    RateLimitResult tryConsume(String bucketKey, RateLimitPolicy policy);
}
