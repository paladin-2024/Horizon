package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class AuthPurgerTest {

    @Autowired
    OtpPurger otpPurger;

    @Autowired
    RefreshTokenPurger refreshTokenPurger;

    @Autowired
    OtpService otpService;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void namesAreStable() {
        assertThat(otpPurger.name()).isEqualTo("otp_codes");
        assertThat(refreshTokenPurger.name()).isEqualTo("refresh_tokens");
    }

    @Test
    void deletesExpiredOtpCodesAndKeepsLiveOnes() {
        String expiredPhone = TestPhones.nextUgandan();
        String livePhone = TestPhones.nextUgandan();
        otpService.issue(expiredPhone);
        otpService.issue(livePhone);
        jdbcTemplate.update("update otp_codes set expires_at = ? where phone = ?",
                Timestamp.from(Instant.now().minusSeconds(3600)), expiredPhone);

        int deleted = otpPurger.purgeBatch(500);

        assertThat(deleted).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from otp_codes where phone = ?", Integer.class, expiredPhone))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from otp_codes where phone = ?", Integer.class, livePhone))
                .isEqualTo(1);
    }

    @Test
    void deletesExpiredRefreshTokensButKeepsRevokedOnesUntilTheyExpire() {
        UUID expiredUser = UUID.randomUUID();
        UUID revokedUser = UUID.randomUUID();
        String expired = refreshTokenService.startFamily(expiredUser);
        String revoked = refreshTokenService.startFamily(revokedUser);
        refreshTokenService.rotate(revoked);
        jdbcTemplate.update("update refresh_tokens set expires_at = ? where user_id = ?",
                Timestamp.from(Instant.now().minusSeconds(3600)), expiredUser);

        int deleted = refreshTokenPurger.purgeBatch(500);

        assertThat(deleted).isPositive();
        assertThat(expired).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where user_id = ?", Integer.class, expiredUser))
                .isZero();
        // the revoked-but-unexpired row must survive: reuse detection depends on it
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where user_id = ? and revoked_at is not null",
                Integer.class, revokedUser)).isEqualTo(1);
    }

    @Test
    void respectsTheBatchSize() {
        String phone = TestPhones.nextUgandan();
        otpService.issue(phone);
        otpService.issue(phone);
        otpService.issue(phone);
        jdbcTemplate.update("update otp_codes set expires_at = ? where phone = ?",
                Timestamp.from(Instant.now().minusSeconds(3600)), phone);

        assertThat(otpPurger.purgeBatch(2)).isEqualTo(2);
        assertThat(otpPurger.purgeBatch(2)).isEqualTo(1);
        assertThat(otpPurger.purgeBatch(2)).isZero();
    }
}
