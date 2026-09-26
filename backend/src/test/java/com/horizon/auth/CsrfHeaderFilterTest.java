package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class CsrfHeaderFilterTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    ApplicationContext context;

    HttpApi api;

    @BeforeEach
    void setUp() {
        api = new HttpApi(port);
    }

    @Test
    void refusesAStateChangingRequestWithoutTheClientHeader() {
        HttpResponse<String> response = api.postRaw("/api/v1/anything", "{}", HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"csrf_header_required\"");
        assertThat(response.body()).contains("\"type\":\"urn:horizon:error:csrf_header_required\"")
                .doesNotContain("about:blank");
        assertThat(response.headers().firstValue("content-type").orElse(""))
                .contains("application/problem+json");
    }

    @Test
    void refusesAWrongClientHeaderValue() {
        HttpResponse<String> response = api.postRaw("/api/v1/anything", "{}",
                HttpApi.headers(CsrfHeaderFilter.HEADER, "curl"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("csrf_header_required");
    }

    @Test
    void letsAStateChangingRequestWithTheHeaderReachAuthorization() {
        HttpResponse<String> response = api.postRaw("/api/v1/anything", "{}",
                HttpApi.headers(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.EXPECTED));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void doesNotAskForTheHeaderOnReads() {
        assertThat(api.get("/actuator/health", HttpApi.headers()).statusCode()).isEqualTo(200);
        assertThat(api.get("/api/v1/anything", HttpApi.headers()).statusCode()).isEqualTo(401);
    }

    /** Replacing SecurityConfig must not reopen what slice 1 closed. */
    @Test
    void actuatorEndpointsOtherThanHealthStayDenied() {
        assertThat(api.get("/actuator/metrics", HttpApi.headers()).statusCode()).isEqualTo(401);
        assertThat(api.get("/actuator/info", HttpApi.headers()).statusCode()).isEqualTo(401);
    }

    /**
     * Boot creates an in-memory user with a generated password (and logs it) when no
     * UserDetailsService exists. Nothing here authenticates through Spring's authentication manager,
     * so that user must not exist (decision D18).
     */
    @Test
    void noInMemoryUserWithAGeneratedPasswordIsCreated() {
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
    }
}
