package com.horizon.common.idempotency;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class IdempotencyWebConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor interceptor;

    IdempotencyWebConfig(IdempotencyInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }

    /** Order 10: after Spring Security, before the rate limit filter. */
    @Bean
    FilterRegistrationBean<CachedBodyRequestFilter> cachedBodyRequestFilterRegistration() {
        FilterRegistrationBean<CachedBodyRequestFilter> registration =
                new FilterRegistrationBean<>(new CachedBodyRequestFilter());
        registration.setOrder(10);
        return registration;
    }
}
