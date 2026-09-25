package com.horizon.common.ratelimit;

import com.horizon.common.error.ApiException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a bucket is empty. Plan 01's {@code GlobalExceptionHandler} renders it as 429 with
 * {@code code=rate_limited}, {@code type=urn:horizon:error:rate_limited} and the {@code Retry-After}
 * header carried in the exception's {@code HttpHeaders}; no extra advice is needed.
 */
public class RateLimitExceededException extends ApiException {

    public static final String CODE = "rate_limited";

    private final transient Duration retryAfter;

    public RateLimitExceededException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, CODE, message(retryAfter), headers(retryAfter));
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    public long retryAfterSeconds() {
        return new RateLimitResult(false, retryAfter).retryAfterSeconds();
    }

    private static HttpHeaders headers(Duration retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(new RateLimitResult(false, retryAfter).retryAfterSeconds()));
        return headers;
    }

    private static String message(Duration retryAfter) {
        return "Too many requests. Try again in " + new RateLimitResult(false, retryAfter).retryAfterSeconds() + " seconds.";
    }
}
