package com.horizon.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "horizon.auth")
public record AuthProperties(
        String jwtSecret,
        String jwtKid,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration otpTtl,
        int otpMaxAttempts,
        String otpPepper) {
}
