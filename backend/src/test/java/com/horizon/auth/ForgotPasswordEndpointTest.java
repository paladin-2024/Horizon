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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class ForgotPasswordEndpointTest {

    private static final String PASSWORD = "Str0ngPassw0rd!";
    private static final String NEW_PASSWORD = "EvenStr0ngerPassw0rd!";

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

    private String registerAndVerify(String phone, String email) {
        api.post("/api/v1/auth/register", """
                {"phone":"%s","password":"%s","firstName":"Ada","lastName":"Lovelace",\
                "country":"UG","email":"%s"}""".formatted(phone, PASSWORD, email),
                HttpApi.headers());
        String code = sms.lastCodeFor(phone);
        api.post("/api/v1/auth/verify-otp",
                """
                {"phone":"%s","code":"%s"}""".formatted(phone, code), HttpApi.headers());
        sms.clear();
        return phone;
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void sendsAResetCodeToTheAccountsPhoneForAKnownVerifiedEmail() {
        String email = uniqueEmail();
        String phone = registerAndVerify(TestPhones.nextUgandan(), email);

        HttpResponse<String> response = api.post("/api/v1/auth/forgot-password",
                """
                {"email":"%s"}""".formatted(email), HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(sms.countFor(phone)).isEqualTo(1);
        assertThat(sms.lastCodeFor(phone)).matches("\\d{6}");
    }

    @Test
    void answersTheSame202ForAnUnknownEmailAndSendsNothing() {
        HttpResponse<String> response = api.post("/api/v1/auth/forgot-password",
                """
                {"email":"%s"}""".formatted(uniqueEmail()), HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(sms.sent()).isEmpty();
    }

    @Test
    void resetsThePasswordWithAValidCodeAndThenAcceptsTheNewOne() {
        String email = uniqueEmail();
        String phone = registerAndVerify(TestPhones.nextUgandan(), email);
        api.post("/api/v1/auth/forgot-password", """
                {"email":"%s"}""".formatted(email), HttpApi.headers());
        String code = sms.lastCodeFor(phone);

        HttpResponse<String> reset = api.post("/api/v1/auth/reset-password",
                """
                {"email":"%s","code":"%s","newPassword":"%s"}""".formatted(email, code, NEW_PASSWORD),
                HttpApi.headers());

        assertThat(reset.statusCode()).isEqualTo(204);

        HttpResponse<String> oldPasswordLogin = api.post("/api/v1/auth/login",
                """
                {"identifier":"%s","password":"%s"}""".formatted(email, PASSWORD), HttpApi.headers());
        HttpResponse<String> newPasswordLogin = api.post("/api/v1/auth/login",
                """
                {"identifier":"%s","password":"%s"}""".formatted(email, NEW_PASSWORD), HttpApi.headers());

        assertThat(oldPasswordLogin.statusCode()).isEqualTo(401);
        assertThat(newPasswordLogin.statusCode()).isEqualTo(200);
    }

    @Test
    void rejectsAWrongOrReusedCode() {
        String email = uniqueEmail();
        String phone = registerAndVerify(TestPhones.nextUgandan(), email);
        api.post("/api/v1/auth/forgot-password", """
                {"email":"%s"}""".formatted(email), HttpApi.headers());
        String code = sms.lastCodeFor(phone);
        String wrong = code.equals("000000") ? "111111" : "000000";

        HttpResponse<String> wrongCode = api.post("/api/v1/auth/reset-password",
                """
                {"email":"%s","code":"%s","newPassword":"%s"}""".formatted(email, wrong, NEW_PASSWORD),
                HttpApi.headers());
        assertThat(wrongCode.statusCode()).isEqualTo(400);
        assertThat(wrongCode.body()).contains("\"code\":\"invalid_otp\"");

        HttpResponse<String> firstUse = api.post("/api/v1/auth/reset-password",
                """
                {"email":"%s","code":"%s","newPassword":"%s"}""".formatted(email, code, NEW_PASSWORD),
                HttpApi.headers());
        assertThat(firstUse.statusCode()).isEqualTo(204);

        HttpResponse<String> reuse = api.post("/api/v1/auth/reset-password",
                """
                {"email":"%s","code":"%s","newPassword":"AnotherPassw0rd!"}""".formatted(email, code),
                HttpApi.headers());
        assertThat(reuse.statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsAnUnknownEmailWithTheSameGenericError() {
        HttpResponse<String> response = api.post("/api/v1/auth/reset-password",
                """
                {"email":"%s","code":"123456","newPassword":"%s"}""".formatted(uniqueEmail(), NEW_PASSWORD),
                HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"invalid_otp\"");
    }

    @Test
    void doesNotSendACodeForAnUnverifiedPhone() {
        String email = uniqueEmail();
        String phone = TestPhones.nextUgandan();
        api.post("/api/v1/auth/register", """
                {"phone":"%s","password":"%s","firstName":"Ada","lastName":"Lovelace",\
                "country":"UG","email":"%s"}""".formatted(phone, PASSWORD, email),
                HttpApi.headers());
        sms.clear();

        HttpResponse<String> response = api.post("/api/v1/auth/forgot-password",
                """
                {"email":"%s"}""".formatted(email), HttpApi.headers());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(sms.sent()).isEmpty();
    }

    @Test
    void writesAnAuditEntryForTheReset() {
        String email = uniqueEmail();
        String phone = registerAndVerify(TestPhones.nextUgandan(), email);
        api.post("/api/v1/auth/forgot-password", """
                {"email":"%s"}""".formatted(email), HttpApi.headers());
        String code = sms.lastCodeFor(phone);

        api.post("/api/v1/auth/reset-password",
                """
                {"email":"%s","code":"%s","newPassword":"%s"}""".formatted(email, code, NEW_PASSWORD),
                HttpApi.headers());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'auth.reset_password'", Integer.class))
                .isPositive();
    }
}
