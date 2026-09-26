package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class OtpServiceTest {

    @Autowired
    OtpService otpService;

    @Autowired
    SmsTestConfig.RecordingSmsSender sms;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearSms() {
        sms.clear();
    }

    @Test
    void sendsASixDigitCodeAndStoresItHashed() {
        String phone = TestPhones.nextUgandan();

        otpService.issue(phone);

        String code = sms.lastCodeFor(phone);
        assertThat(code).matches("\\d{6}");
        assertThat(sms.lastMessageFor(phone).orElseThrow()).contains(code);
        String storedHash = jdbcTemplate.queryForObject(
                "select code_hash from otp_codes where phone = ?", String.class, phone);
        assertThat(storedHash).isNotNull().doesNotContain(code).hasSize(64);
    }

    @Test
    void acceptsTheCodeExactlyOnce() {
        String phone = TestPhones.nextUgandan();
        otpService.issue(phone);
        String code = sms.lastCodeFor(phone);

        assertThat(otpService.consume(phone, code)).isTrue();
        assertThat(otpService.consume(phone, code)).isFalse();
    }

    @Test
    void rejectsAWrongCodeAndLocksOutAfterFiveAttempts() {
        String phone = TestPhones.nextUgandan();
        otpService.issue(phone);
        String code = sms.lastCodeFor(phone);

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(otpService.consume(phone, "000000".equals(code) ? "111111" : "000000")).isFalse();
        }

        assertThat(otpService.consume(phone, code)).isFalse();
        Integer attempts = jdbcTemplate.queryForObject(
                "select attempts from otp_codes where phone = ?", Integer.class, phone);
        assertThat(attempts).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from otp_codes where phone = ? and consumed_at is not null",
                Integer.class, phone)).isEqualTo(1);
    }

    @Test
    void rejectsAnExpiredCode() {
        String phone = TestPhones.nextUgandan();
        otpService.issue(phone);
        String code = sms.lastCodeFor(phone);
        jdbcTemplate.update("update otp_codes set expires_at = ? where phone = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), phone);

        assertThat(otpService.consume(phone, code)).isFalse();
    }

    @Test
    void rejectsACodeForAPhoneThatNeverAskedForOne() {
        assertThat(otpService.consume(TestPhones.nextCongolese(), "123456")).isFalse();
    }

    @Test
    void issuingAgainInvalidatesThePreviousCode() {
        String phone = TestPhones.nextUgandan();
        otpService.issue(phone);
        String first = sms.lastCodeFor(phone);
        otpService.issue(phone);
        String second = sms.lastCodeFor(phone);

        assertThat(second).isNotEqualTo(first);
        assertThat(otpService.consume(phone, first)).isFalse();
        assertThat(otpService.consume(phone, second)).isTrue();
    }
}
