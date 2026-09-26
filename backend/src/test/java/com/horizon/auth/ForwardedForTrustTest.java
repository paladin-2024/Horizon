package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "horizon.security.trust-forwarded-for=false")
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class ForwardedForTrustTest {

    @Value("${local.server.port}")
    int port;

    HttpApi api;

    @BeforeEach
    void setUp() {
        api = new HttpApi(port);
    }

    @Test
    void aForgedForwardedForHeaderDoesNotGiveAFreshLoginBudget() {
        int limitedAt = 0;
        for (int attempt = 1; attempt <= 8 && limitedAt == 0; attempt++) {
            HttpResponse<String> response = api.post("/api/v1/auth/login",
                    """
                    {"identifier":"nobody@example.com","password":"NotThePassword!"}""",
                    HttpApi.headers("X-Forwarded-For", HttpApi.freshIp()));
            if (response.statusCode() == 429) {
                limitedAt = attempt;
            }
        }

        // Every request comes from this test's own socket, so the sixth attempt is the first refused,
        // although each one claims a different address.
        assertThat(limitedAt).isEqualTo(6);
    }

    @Test
    void aForgedForwardedForHeaderDoesNotGiveAFreshOtpBudget() {
        int limitedAt = 0;
        for (int attempt = 1; attempt <= 12 && limitedAt == 0; attempt++) {
            HttpResponse<String> response = api.post("/api/v1/auth/resend-otp",
                    """
                    {"phone":"%s"}""".formatted(TestPhones.nextUgandan()),
                    HttpApi.headers("X-Forwarded-For", HttpApi.freshIp()));
            if (response.statusCode() == 429) {
                limitedAt = attempt;
            }
        }

        assertThat(limitedAt).isEqualTo(11);
    }
}
