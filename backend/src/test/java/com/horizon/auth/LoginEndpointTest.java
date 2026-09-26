package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class LoginEndpointTest {

    private static final String PASSWORD = "Str0ngPassw0rd!";

    @Value("${local.server.port}")
    int port;

    @Autowired
    SmsTestConfig.RecordingSmsSender sms;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JwtService jwtService;

    HttpApi api;

    @BeforeEach
    void setUp() {
        api = new HttpApi(port);
        sms.clear();
    }

    /**
     * Each call speaks from its own IP (the first X-Forwarded-For hop; the test profile trusts the
     * header), so the 5-per-minute per-IP limit never crosses tests.
     */
    private Map<String, String> fromFreshIp() {
        return HttpApi.headers("X-Forwarded-For", HttpApi.freshIp() + ", 198.51.100.1");
    }

    /** A JWT signed by us but two hours past its expiry, as a browser would still send it (D3). */
    private String expiredAccessJwt() {
        return jwtService.issueAccessTokenAt(UUID.randomUUID(),
                java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS));
    }

    private String registerAndVerify(String phone, String email) {
        api.post("/api/v1/auth/register", """
                {"phone":"%s","password":"%s","firstName":"Ada","lastName":"Lovelace",\
                "country":"UG","email":"%s"}""".formatted(phone, PASSWORD, email),
                HttpApi.headers());
        String code = sms.lastCodeFor(phone);
        HttpResponse<String> verified = api.post("/api/v1/auth/verify-otp", """
                {"phone":"%s","code":"%s"}""".formatted(phone, code), HttpApi.headers());
        assertThat(verified.statusCode()).isEqualTo(200);
        return HttpApi.cookieValue(verified, "hz_refresh").orElseThrow();
    }

    private String registerOnly(String phone, String email) {
        api.post("/api/v1/auth/register", """
                {"phone":"%s","password":"%s","firstName":"Ada","lastName":"Lovelace",\
                "country":"UG","email":"%s"}""".formatted(phone, PASSWORD, email),
                HttpApi.headers());
        return phone;
    }

    private HttpResponse<String> login(String identifier, String password,
            Map<String, String> headers) {
        return api.post("/api/v1/auth/login", """
                {"identifier":"%s","password":"%s"}""".formatted(identifier, password), headers);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void signsInWithThePhoneAndWithTheEmail() {
        String phone = TestPhones.nextUgandan();
        String email = uniqueEmail();
        registerAndVerify(phone, email);

        HttpResponse<String> byPhone = login(phone, PASSWORD, fromFreshIp());
        HttpResponse<String> byEmail = login(email.toUpperCase(java.util.Locale.ROOT), PASSWORD,
                fromFreshIp());

        assertThat(byPhone.statusCode()).isEqualTo(200);
        assertThat(HttpApi.jsonString(byPhone.body(), "phone")).isEqualTo(phone);
        assertThat(HttpApi.setCookie(byPhone, "hz_access").orElseThrow())
                .contains("HttpOnly").contains("SameSite=Strict").contains("Max-Age=2592000");
        assertThat(byEmail.statusCode()).isEqualTo(200);
    }

    @Test
    void givesOneGenericErrorForAWrongPasswordAndForAnUnknownAccount() {
        String phone = TestPhones.nextUgandan();
        registerAndVerify(phone, uniqueEmail());

        HttpResponse<String> wrongPassword = login(phone, "NotThePassword!", fromFreshIp());
        HttpResponse<String> unknownPhone =
                login(TestPhones.nextUgandan(), PASSWORD, fromFreshIp());
        HttpResponse<String> unknownEmail = login(uniqueEmail(), PASSWORD, fromFreshIp());

        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(unknownPhone.statusCode()).isEqualTo(401);
        assertThat(unknownEmail.statusCode()).isEqualTo(401);
        assertThat(wrongPassword.body()).contains("\"code\":\"invalid_credentials\"");
        assertThat(unknownPhone.body()).isEqualTo(wrongPassword.body());
        assertThat(unknownEmail.body()).isEqualTo(wrongPassword.body());
        assertThat(HttpApi.setCookie(wrongPassword, "hz_access")).isEmpty();
    }

    @Test
    void tellsAnUnverifiedUserToVerifyThePhone() {
        String phone = registerOnly(TestPhones.nextUgandan(), uniqueEmail());

        HttpResponse<String> response = login(phone, PASSWORD, fromFreshIp());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"phone_not_verified\"");
        assertThat(HttpApi.setCookie(response, "hz_access")).isEmpty();
    }

    @Test
    void rateLimitsLoginsFromOneIpAddress() {
        String phone = TestPhones.nextUgandan();
        registerAndVerify(phone, uniqueEmail());
        Map<String, String> oneIp = HttpApi.headers("X-Forwarded-For", "198.51.100.77");

        int lastStatus = 0;
        for (int attempt = 1; attempt <= 8 && lastStatus != 429; attempt++) {
            lastStatus = login(phone, "NotThePassword!", oneIp).statusCode();
        }

        assertThat(lastStatus).isEqualTo(429);
        HttpResponse<String> limited = login(phone, "NotThePassword!", oneIp);
        assertThat(limited.headers().firstValue("retry-after")).isPresent();
    }

    @Test
    void rotatesTheRefreshTokenAndTheAccessCookie() {
        String phone = TestPhones.nextUgandan();
        String refresh = registerAndVerify(phone, uniqueEmail());

        HttpResponse<String> response = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", refresh)));

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        String rotated = HttpApi.cookieValue(response, "hz_refresh").orElseThrow();
        assertThat(rotated).isNotEqualTo(refresh);
        assertThat(HttpApi.cookieValue(response, "hz_access")).isPresent();
    }

    @Test
    void reusingARefreshTokenRevokesTheWholeFamily() {
        String phone = TestPhones.nextUgandan();
        String first = registerAndVerify(phone, uniqueEmail());
        HttpResponse<String> rotated = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", first)));
        String second = HttpApi.cookieValue(rotated, "hz_refresh").orElseThrow();

        HttpResponse<String> reuse = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", first)));
        HttpResponse<String> afterReuse = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", second)));

        assertThat(reuse.statusCode()).isEqualTo(401);
        assertThat(reuse.body()).contains("\"code\":\"refresh_token_reused\"");
        assertThat(afterReuse.statusCode()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'auth.refresh.reuse_detected'",
                Integer.class)).isPositive();
    }

    @Test
    void refusesToRefreshWithoutOrWithAnUnknownCookie() {
        HttpResponse<String> none = api.post("/api/v1/auth/refresh", "", HttpApi.headers());
        HttpResponse<String> unknown = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", "not-a-real-token")));

        assertThat(none.statusCode()).isEqualTo(401);
        assertThat(none.body()).contains("\"code\":\"invalid_refresh_token\"");
        assertThat(unknown.statusCode()).isEqualTo(401);
    }

    @Test
    void logoutClearsBothCookiesAndKillsTheFamily() {
        String phone = TestPhones.nextUgandan();
        String refresh = registerAndVerify(phone, uniqueEmail());

        HttpResponse<String> response = api.post("/api/v1/auth/logout", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", refresh)));

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(HttpApi.setCookie(response, "hz_access").orElseThrow()).contains("Max-Age=0");
        assertThat(HttpApi.setCookie(response, "hz_refresh").orElseThrow())
                .contains("Max-Age=0").contains("Path=/api/v1/auth");
        assertThat(api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", refresh)))
                .statusCode()).isEqualTo(401);
    }

    /** D5: refresh works with only the refresh cookie, so a stale (expired) access JWT cannot block it. */
    @Test
    void refreshNeedsOnlyTheRefreshCookieEvenWhenTheAccessJwtHasExpired() {
        String refresh = registerAndVerify(TestPhones.nextUgandan(), uniqueEmail());

        HttpResponse<String> response = api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie",
                        HttpApi.cookieHeader("hz_access", expiredAccessJwt(), "hz_refresh", refresh)));

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertThat(HttpApi.setCookie(response, "hz_access").orElseThrow()).contains("Max-Age=2592000");
        assertThat(HttpApi.cookieValue(response, "hz_refresh")).isPresent();
    }

    /** D5: logout clears both cookies and revokes the family even when the access JWT has expired. */
    @Test
    void logoutClearsBothCookiesEvenWhenTheAccessJwtHasExpired() {
        String refresh = registerAndVerify(TestPhones.nextUgandan(), uniqueEmail());
        String bothCookies = HttpApi.cookieHeader("hz_access", expiredAccessJwt(), "hz_refresh", refresh);

        HttpResponse<String> response = api.post("/api/v1/auth/logout", "", HttpApi.headers("Cookie", bothCookies));

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertThat(HttpApi.setCookie(response, "hz_access").orElseThrow()).contains("Max-Age=0");
        assertThat(HttpApi.setCookie(response, "hz_refresh").orElseThrow()).contains("Max-Age=0");
        assertThat(api.post("/api/v1/auth/refresh", "",
                HttpApi.headers("Cookie", HttpApi.cookieHeader("hz_refresh", refresh))).statusCode())
                .isEqualTo(401);
    }

    @Test
    void logoutWithoutACookieStillAnswers204() {
        assertThat(api.post("/api/v1/auth/logout", "", HttpApi.headers()).statusCode())
                .isEqualTo(204);
    }

    @Test
    void writesAuditEntriesForSuccessfulAndFailedLogins() {
        String phone = TestPhones.nextUgandan();
        registerAndVerify(phone, uniqueEmail());
        login(phone, PASSWORD, fromFreshIp());
        login(phone, "NotThePassword!", fromFreshIp());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'auth.login'", Integer.class))
                .isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'auth.login.failed'", Integer.class))
                .isPositive();
    }
}
