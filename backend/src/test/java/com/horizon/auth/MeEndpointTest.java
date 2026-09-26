package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class MeEndpointTest {

    private static final String PASSWORD = "Str0ngPassw0rd!";

    @Value("${local.server.port}")
    int port;

    @Autowired
    SmsTestConfig.RecordingSmsSender sms;

    @Autowired
    JwtService jwtService;

    HttpApi api;

    @BeforeEach
    void setUp() {
        api = new HttpApi(port);
        sms.clear();
    }

    private HttpResponse<String> verifiedSession(String phone) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        api.post("/api/v1/auth/register", """
                {"phone":"%s","password":"%s","firstName":"Ada","lastName":"Lovelace",\
                "country":"UG","email":"%s"}""".formatted(phone, PASSWORD, email),
                HttpApi.headers());
        return api.post("/api/v1/auth/verify-otp", """
                {"phone":"%s","code":"%s"}""".formatted(phone, sms.lastCodeFor(phone)),
                HttpApi.headers());
    }

    @Test
    void returnsTheSignedInUser() {
        String phone = TestPhones.nextUgandan();
        HttpResponse<String> session = verifiedSession(phone);
        String access = HttpApi.cookieValue(session, "hz_access").orElseThrow();

        HttpResponse<String> me = api.get("/api/v1/auth/me",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_access", access)));

        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(HttpApi.jsonString(me.body(), "phone")).isEqualTo(phone);
        assertThat(HttpApi.jsonString(me.body(), "firstName")).isEqualTo("Ada");
        assertThat(HttpApi.jsonString(me.body(), "country")).isEqualTo("UG");
        assertThat(HttpApi.jsonString(me.body(), "language")).isEqualTo("en");
        assertThat(me.body()).doesNotContain("passwordHash").doesNotContain("nationalId");
        assertThat(HttpApi.jsonString(me.body(), "id")).isEqualTo(
                HttpApi.jsonString(session.body(), "id"));
    }

    @Test
    void refusesWithoutACookieWithAGarbageCookieAndWithAnExpiredToken() {
        assertThat(api.get("/api/v1/auth/me", HttpApi.headers()).statusCode()).isEqualTo(401);
        assertThat(api.get("/api/v1/auth/me",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_access", "not-a-jwt")))
                .statusCode()).isEqualTo(401);

        String expired = jwtService.issueAccessTokenAt(UUID.randomUUID(),
                java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS));
        assertThat(api.get("/api/v1/auth/me",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_access", expired)))
                .statusCode()).isEqualTo(401);
    }

    @Test
    void aTokenForADeletedOrUnknownUserIsRefused() {
        String orphan = jwtService.issueAccessToken(UUID.randomUUID());

        HttpResponse<String> me = api.get("/api/v1/auth/me",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_access", orphan)));

        assertThat(me.statusCode()).isEqualTo(401);
        assertThat(me.body()).contains("\"code\":\"unauthenticated\"");
    }

    @Test
    void theRotatedAccessCookieAlsoWorks() {
        String phone = TestPhones.nextUgandan();
        HttpResponse<String> session = verifiedSession(phone);
        String refresh = HttpApi.cookieValue(session, "hz_refresh").orElseThrow();
        HttpResponse<String> rotated = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", refresh)));
        String access = HttpApi.cookieValue(rotated, "hz_access").orElseThrow();

        assertThat(api.get("/api/v1/auth/me",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_access", access)))
                .statusCode()).isEqualTo(200);
    }

    /** D3: the cookie outlives the 15-minute JWT, so a stale JWT keeps arriving; 401, then refresh recovers. */
    @Test
    void aStaleJwtInsideAStillSentCookieIs401AndRefreshRecoversTheSession() {
        HttpResponse<String> session = verifiedSession(TestPhones.nextUgandan());
        String refresh = HttpApi.cookieValue(session, "hz_refresh").orElseThrow();
        UUID userId = UUID.fromString(HttpApi.jsonString(session.body(), "id"));
        String stale = jwtService.issueAccessTokenAt(userId,
                java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS));
        String bothCookies = HttpApi.cookieHeader("hz_access", stale, "hz_refresh", refresh);

        assertThat(api.get("/api/v1/auth/me", HttpApi.headers("Cookie", bothCookies)).statusCode())
                .isEqualTo(401);

        HttpResponse<String> refreshed = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", bothCookies));
        assertThat(refreshed.statusCode()).isEqualTo(204);
        String fresh = HttpApi.cookieValue(refreshed, "hz_access").orElseThrow();
        assertThat(api.get("/api/v1/auth/me",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_access", fresh))).statusCode())
                .isEqualTo(200);
    }

    @Test
    void logoutDoesNotNeedAValidAccessTokenButMeDoes() {
        assertThat(api.post("/api/v1/auth/logout", "", HttpApi.headers()).statusCode())
                .isEqualTo(204);
        assertThat(api.get("/api/v1/auth/me", HttpApi.headers()).statusCode()).isEqualTo(401);
    }
}
