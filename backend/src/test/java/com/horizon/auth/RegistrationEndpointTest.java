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
class RegistrationEndpointTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    SmsTestConfig.RecordingSmsSender sms;

    @Autowired
    JdbcTemplate jdbcTemplate;

    HttpApi api;

    @BeforeEach
    void setUp() {
        api = new HttpApi(port);
        sms.clear();
    }

    private static String registerBody(String phone, String email) {
        return """
                {"phone":"%s","password":"Str0ngPassw0rd!","firstName":"Ada","lastName":"Lovelace",\
                "country":"UG","email":%s,"nationalId":"CM123456"}"""
                .formatted(phone, email == null ? "null" : "\"" + email + "\"");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void registersAnUnverifiedUserAndSendsTheCode() {
        String phone = TestPhones.nextUgandan();

        HttpResponse<String> response =
                api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(sms.countFor(phone)).isEqualTo(1);
        assertThat(sms.lastCodeFor(phone)).matches("\\d{6}");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where phone = ? and phone_verified_at is null",
                Integer.class, phone)).isEqualTo(1);
    }

    @Test
    void answersTheSame202ForAPhoneThatAlreadyExists() {
        String phone = TestPhones.nextUgandan();
        HttpResponse<String> first =
                api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());
        sms.clear();

        HttpResponse<String> second =
                api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());

        assertThat(first.statusCode()).isEqualTo(202);
        assertThat(second.statusCode()).isEqualTo(202);
        assertThat(second.body()).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where phone = ?", Integer.class, phone)).isEqualTo(1);
        // still unverified, so a fresh code is sent instead of leaking that the account exists
        assertThat(sms.countFor(phone)).isEqualTo(1);
    }

    @Test
    void answersTheSame202ForAnEmailThatAlreadyExists() {
        String email = uniqueEmail();
        api.post("/api/v1/auth/register", registerBody(TestPhones.nextUgandan(), email),
                HttpApi.headers());
        String otherPhone = TestPhones.nextUgandan();

        HttpResponse<String> response =
                api.post("/api/v1/auth/register", registerBody(otherPhone, email), HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where phone = ?", Integer.class, otherPhone)).isZero();
    }

    @Test
    void rejectsAnInvalidPhoneAndAMissingIdempotencyKey() {
        HttpResponse<String> badPhone = api.post("/api/v1/auth/register",
                registerBody("+254712345678", uniqueEmail()), HttpApi.headers());
        assertThat(badPhone.statusCode()).isEqualTo(400);
        assertThat(badPhone.body()).contains("\"code\":\"invalid_phone\"");

        HttpResponse<String> noKey = api.postRaw("/api/v1/auth/register",
                registerBody(TestPhones.nextUgandan(), uniqueEmail()),
                HttpApi.headers(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.EXPECTED));
        assertThat(noKey.statusCode()).isEqualTo(400);
        assertThat(noKey.body()).contains("idempotency_key_required");

        HttpResponse<String> noCsrf = api.postRaw("/api/v1/auth/register",
                registerBody(TestPhones.nextUgandan(), uniqueEmail()),
                HttpApi.headers("Idempotency-Key", UUID.randomUUID().toString()));
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        assertThat(noCsrf.body()).contains("csrf_header_required");
    }

    @Test
    void rejectsAShortPasswordAndAMissingName() {
        String tooShort = """
                {"phone":"%s","password":"short","firstName":"Ada","lastName":"Lovelace","country":"UG"}"""
                .formatted(TestPhones.nextUgandan());

        HttpResponse<String> response =
                api.post("/api/v1/auth/register", tooShort, HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("validation_failed");
    }

    @Test
    void replaysTheStoredResponseForTheSameIdempotencyKey() {
        String phone = TestPhones.nextUgandan();
        String body = registerBody(phone, uniqueEmail());
        String key = UUID.randomUUID().toString();

        HttpResponse<String> first =
                api.post("/api/v1/auth/register", body, HttpApi.headers("Idempotency-Key", key));
        HttpResponse<String> replay =
                api.post("/api/v1/auth/register", body, HttpApi.headers("Idempotency-Key", key));

        assertThat(first.statusCode()).isEqualTo(202);
        assertThat(replay.statusCode()).isEqualTo(202);
        assertThat(sms.countFor(phone)).isEqualTo(1);
    }

    @Test
    void verifiesThePhoneAndSetsBothCookies() {
        String phone = TestPhones.nextUgandan();
        api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());
        String code = sms.lastCodeFor(phone);

        HttpResponse<String> response = api.post("/api/v1/auth/verify-otp",
                """
                {"phone":"%s","code":"%s"}""".formatted(phone, code), HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(HttpApi.jsonString(response.body(), "phone")).isEqualTo(phone);
        assertThat(HttpApi.jsonString(response.body(), "country")).isEqualTo("UG");
        assertThat(HttpApi.jsonString(response.body(), "language")).isEqualTo("en");
        assertThat(response.body()).doesNotContain("passwordHash").doesNotContain("nationalId");

        String access = HttpApi.setCookie(response, "hz_access").orElseThrow();
        assertThat(access).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/");
        assertThat(access).doesNotContain("Secure");
        // D3: the cookie lives as long as the refresh token (30 days) although the JWT inside lasts 15 minutes
        assertThat(access).contains("Max-Age=2592000");
        String refresh = HttpApi.setCookie(response, "hz_refresh").orElseThrow();
        assertThat(refresh).contains("HttpOnly").contains("SameSite=Strict")
                .contains("Path=/api/v1/auth").contains("Max-Age=2592000");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where phone = ? and phone_verified_at is not null",
                Integer.class, phone)).isEqualTo(1);
    }

    @Test
    void rejectsAWrongCodeAndLocksOutAfterFiveTries() {
        String phone = TestPhones.nextUgandan();
        api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());
        String code = sms.lastCodeFor(phone);
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int attempt = 1; attempt <= 5; attempt++) {
            HttpResponse<String> response = api.post("/api/v1/auth/verify-otp",
                    """
                    {"phone":"%s","code":"%s"}""".formatted(phone, wrong), HttpApi.headers());
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("\"code\":\"invalid_otp\"");
        }

        HttpResponse<String> afterLockout = api.post("/api/v1/auth/verify-otp",
                """
                {"phone":"%s","code":"%s"}""".formatted(phone, code), HttpApi.headers());
        assertThat(afterLockout.statusCode()).isEqualTo(400);
        assertThat(HttpApi.setCookie(afterLockout, "hz_access")).isEmpty();
    }

    @Test
    void givesTheSameAnswerForAnUnknownPhone() {
        HttpResponse<String> response = api.post("/api/v1/auth/verify-otp",
                """
                {"phone":"%s","code":"123456"}""".formatted(TestPhones.nextCongolese()),
                HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"invalid_otp\"");
    }

    @Test
    void resendsACodeAndAlwaysAnswers202() {
        String phone = TestPhones.nextUgandan();
        api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());
        sms.clear();

        HttpResponse<String> known = api.post("/api/v1/auth/resend-otp",
                """
                {"phone":"%s"}""".formatted(phone), HttpApi.headers());
        String unknownPhone = TestPhones.nextCongolese();
        HttpResponse<String> unknown = api.post("/api/v1/auth/resend-otp",
                """
                {"phone":"%s"}""".formatted(unknownPhone), HttpApi.headers());

        assertThat(known.statusCode()).isEqualTo(202);
        assertThat(unknown.statusCode()).isEqualTo(202);
        assertThat(sms.countFor(phone)).isEqualTo(1);
        assertThat(sms.countFor(unknownPhone)).isZero();
    }

    @Test
    void rateLimitsOtpSendsPerPhone() {
        String phone = TestPhones.nextUgandan();
        api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());

        int lastStatus = 202;
        for (int i = 0; i < 5 && lastStatus != 429; i++) {
            lastStatus = api.post("/api/v1/auth/resend-otp",
                    """
                    {"phone":"%s"}""".formatted(phone), HttpApi.headers()).statusCode();
        }

        assertThat(lastStatus).isEqualTo(429);
    }

    /** Decision D14: ten OTP sends per hour per client IP, however many phone numbers are used. */
    @Test
    void rateLimitsOtpSendsPerClientIpAcrossDifferentPhoneNumbers() {
        Map<String, String> oneIp = HttpApi.headers("X-Forwarded-For", HttpApi.freshIp());

        int accepted = 0;
        int lastStatus = 0;
        for (int attempt = 1; attempt <= 12 && lastStatus != 429; attempt++) {
            lastStatus = api.post("/api/v1/auth/register",
                    registerBody(TestPhones.nextUgandan(), uniqueEmail()), oneIp).statusCode();
            if (lastStatus == 202) {
                accepted++;
            }
        }

        assertThat(accepted).isEqualTo(10);
        assertThat(lastStatus).isEqualTo(429);
    }

    /** The API and the web form (plan 06's zod schema) agree on a minimum of 8 characters. */
    @Test
    void requiresAtLeastEightCharactersInThePassword() {
        String sevenChars = """
                {"phone":"%s","password":"1234567","firstName":"Ada","lastName":"Lovelace","country":"UG"}"""
                .formatted(TestPhones.nextUgandan());
        String eightChars = """
                {"phone":"%s","password":"12345678","firstName":"Ada","lastName":"Lovelace","country":"UG"}"""
                .formatted(TestPhones.nextUgandan());

        HttpResponse<String> rejected = api.post("/api/v1/auth/register", sevenChars, HttpApi.headers());
        HttpResponse<String> accepted = api.post("/api/v1/auth/register", eightChars, HttpApi.headers());

        assertThat(rejected.statusCode()).isEqualTo(400);
        assertThat(rejected.body()).contains("\"code\":\"validation_failed\"");
        assertThat(accepted.statusCode()).isEqualTo(202);
    }

    @Test
    void writesAnAuditEntryForRegistrationAndVerification() {
        String phone = TestPhones.nextUgandan();
        api.post("/api/v1/auth/register", registerBody(phone, uniqueEmail()), HttpApi.headers());
        api.post("/api/v1/auth/verify-otp",
                """
                {"phone":"%s","code":"%s"}""".formatted(phone, sms.lastCodeFor(phone)),
                HttpApi.headers());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'auth.register'", Integer.class))
                .isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'auth.verify_otp'", Integer.class))
                .isPositive();
    }
}
