package com.horizon.common.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code lockTimeout} bounds how long a crashed in-flight request can hold a key before another
 * request may take it over. {@code retention} is how long a completed response stays replayable
 * (48 hours, per the spec).
 */
@ConfigurationProperties("horizon.idempotency")
public record IdempotencyProperties(Duration lockTimeout, Duration retention) {

    public IdempotencyProperties {
        if (lockTimeout == null) {
            lockTimeout = Duration.ofSeconds(60);
        }
        if (retention == null) {
            retention = Duration.ofHours(48);
        }
    }
}
