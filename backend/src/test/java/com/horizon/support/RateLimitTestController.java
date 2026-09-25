package com.horizon.support;

import com.horizon.common.ratelimit.RateLimitGuard;
import com.horizon.common.ratelimit.RateLimitPolicy;
import java.time.Duration;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Fake endpoint used to prove the 429 response shape end to end. Registered with {@code @Import}. */
@RestController
@RequestMapping("/api/v1/test")
public class RateLimitTestController {

    private static final RateLimitPolicy TWO_PER_TEN_MINUTES = new RateLimitPolicy(2, Duration.ofMinutes(10));

    private final RateLimitGuard guard;

    public RateLimitTestController(RateLimitGuard guard) {
        this.guard = guard;
    }

    @PostMapping("/rate-limited")
    public Map<String, String> rateLimited(@RequestParam String bucket) {
        guard.check("test:" + bucket, TWO_PER_TEN_MINUTES);
        return Map.of("status", "ok");
    }
}
