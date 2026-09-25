package com.horizon.common.ratelimit;

import java.time.Duration;

/** The named limits from the design spec, plus the per-IP OTP cap added during reconciliation (decision D14). */
public final class RateLimitPolicies {

    /** OTP sends: 3 per 10 minutes, keyed by phone number. */
    public static final RateLimitPolicy OTP_SEND = new RateLimitPolicy(3, Duration.ofMinutes(10));

    /** OTP sends: 10 per hour, keyed by client IP, so one host cannot walk through many phone numbers. */
    public static final RateLimitPolicy OTP_SEND_PER_IP = new RateLimitPolicy(10, Duration.ofHours(1));

    /** Login attempts: 5 per minute, keyed by client IP. */
    public static final RateLimitPolicy LOGIN_PER_IP = new RateLimitPolicy(5, Duration.ofMinutes(1));

    /** Login attempts: 5 per minute, keyed by account identifier. */
    public static final RateLimitPolicy LOGIN_PER_ACCOUNT = new RateLimitPolicy(5, Duration.ofMinutes(1));

    /** Everything else: 100 per minute, keyed by authenticated user. */
    public static final RateLimitPolicy GENERAL_PER_USER = new RateLimitPolicy(100, Duration.ofMinutes(1));

    private RateLimitPolicies() {}
}
