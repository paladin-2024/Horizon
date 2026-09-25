package com.horizon.common.ratelimit;

import com.horizon.common.error.ProblemWriter;
import com.horizon.common.security.OptionalCurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies the general per-user budget to every authenticated request. Exceptions thrown from a
 * servlet filter never reach {@code @RestControllerAdvice}, so this filter answers through
 * {@link ProblemWriter}, plan 01's single writer, which produces the same shape the advice does
 * (including {@code type = urn:horizon:error:rate_limited}). Registered after Spring Security's
 * chain, so plan 03's JWT filter has already authenticated the request when this runs.
 */
public class GeneralRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final RateLimitPolicy policy;

    public GeneralRateLimitFilter(RateLimiter rateLimiter, RateLimitPolicy policy) {
        this.rateLimiter = rateLimiter;
        this.policy = policy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<UUID> userId = OptionalCurrentUser.userId();
        if (userId.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        RateLimitResult result = rateLimiter.tryConsume("user:" + userId.get(), policy);
        if (result.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        long seconds = result.retryAfterSeconds();
        ProblemWriter.write(
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                RateLimitExceededException.CODE,
                "Too many requests. Try again in " + seconds + " seconds.",
                Map.of("Retry-After", Long.toString(seconds)));
    }
}
