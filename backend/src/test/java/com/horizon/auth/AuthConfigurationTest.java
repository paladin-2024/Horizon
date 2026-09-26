package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthConfigurationTest {

    @Autowired
    AuthProperties properties;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void propertiesAreBound() {
        assertThat(properties.issuer()).isEqualTo("horizon");
        assertThat(properties.jwtKid()).isEqualTo("test");
        assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.refreshTokenTtl()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.otpTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.otpMaxAttempts()).isEqualTo(5);
        assertThat(properties.jwtSecret()).isNotBlank();
        assertThat(properties.otpPepper()).isNotBlank();
    }

    @Test
    void passwordEncoderIsArgon2id() {
        String hash = passwordEncoder.encode("Str0ngPassw0rd!");

        assertThat(hash).startsWith("$argon2id$");
        assertThat(passwordEncoder.matches("Str0ngPassw0rd!", hash)).isTrue();
        assertThat(passwordEncoder.matches("wrong", hash)).isFalse();
    }
}
