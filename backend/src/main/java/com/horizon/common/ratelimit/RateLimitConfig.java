package com.horizon.common.ratelimit;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RateLimitConfig {

    /**
     * Order 20: after Spring Security's chain (registered at -100), which includes plan 03's
     * JwtAuthenticationFilter, so the SecurityContext already holds the user id.
     */
    @Bean
    FilterRegistrationBean<GeneralRateLimitFilter> generalRateLimitFilterRegistration(RateLimiter rateLimiter) {
        FilterRegistrationBean<GeneralRateLimitFilter> registration =
                new FilterRegistrationBean<>(new GeneralRateLimitFilter(rateLimiter, RateLimitPolicies.GENERAL_PER_USER));
        registration.setOrder(20);
        return registration;
    }
}
