package com.horizon.common.security;

import com.horizon.auth.CsrfHeaderFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /**
     * {@code CsrfHeaderFilter} is a {@code @Component} so it can be constructor-injected below, but
     * that makes Spring Boot also auto-register it as a plain servlet filter on every path (a
     * well-known Spring Security gotcha), running it even for test-only chains such as
     * {@code ResilienceTestSecurity} that are meant to bypass it entirely. This disables only that
     * automatic global registration; {@code addFilterBefore} below is what actually wires it in.
     */
    @Bean
    FilterRegistrationBean<CsrfHeaderFilter> disableAutomaticCsrfHeaderFilterRegistration(
            CsrfHeaderFilter filter) {
        FilterRegistrationBean<CsrfHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfHeaderFilter csrfHeaderFilter)
            throws Exception {
        return http
                // Spring's synchronizer token does not fit a stateless API; CsrfHeaderFilter is the
                // defense instead (see the contracts: X-Horizon-Client on state-changing requests).
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/resend-otp",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(csrfHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
