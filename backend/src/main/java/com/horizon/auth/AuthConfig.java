package com.horizon.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class AuthConfig {

    /**
     * Argon2id with Spring Security's 5.8 defaults (m=16384, t=2, p=1, 16-byte salt, 32-byte hash).
     * Needs org.bouncycastle:bcprov-jdk18on on the classpath.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
