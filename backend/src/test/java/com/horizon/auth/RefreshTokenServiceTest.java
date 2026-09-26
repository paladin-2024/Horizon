package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.horizon.common.error.ApiException;
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
class RefreshTokenServiceTest {

    @Autowired
    RefreshTokenService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private int liveTokensOf(UUID userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where user_id = ? and revoked_at is null",
                Integer.class, userId);
    }

    @Test
    void storesOnlyTheHashOfTheToken() {
        UUID userId = UUID.randomUUID();

        String token = service.startFamily(userId);

        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        assertThat(jdbcTemplate.queryForObject(
                "select token_hash from refresh_tokens where user_id = ?", String.class, userId))
                .isNotNull()
                .doesNotContain(token)
                .hasSize(64);
    }

    @Test
    void rotatesTheTokenAndKeepsTheFamily() {
        UUID userId = UUID.randomUUID();
        String first = service.startFamily(userId);

        RefreshTokenService.Rotation rotation = service.rotate(first);

        assertThat(rotation.userId()).isEqualTo(userId);
        assertThat(rotation.token()).isNotEqualTo(first);
        assertThat(liveTokensOf(userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(distinct family_id) from refresh_tokens where user_id = ?",
                Integer.class, userId)).isEqualTo(1);
        assertThat(service.rotate(rotation.token()).userId()).isEqualTo(userId);
    }

    @Test
    void reusingAnOldTokenRevokesTheWholeFamily() {
        UUID userId = UUID.randomUUID();
        String first = service.startFamily(userId);
        RefreshTokenService.Rotation second = service.rotate(first);

        assertThatThrownBy(() -> service.rotate(first))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reuse");

        assertThat(liveTokensOf(userId)).isZero();
        assertThatThrownBy(() -> service.rotate(second.token())).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsAnUnknownToken() {
        assertThatThrownBy(() -> service.rotate(Tokens.randomToken()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        UUID userId = UUID.randomUUID();
        String token = service.startFamily(userId);
        jdbcTemplate.update("update refresh_tokens set expires_at = ? where user_id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), userId);

        assertThatThrownBy(() -> service.rotate(token)).isInstanceOf(ApiException.class);
    }

    @Test
    void revokingTheFamilyStopsEveryTokenInIt() {
        UUID userId = UUID.randomUUID();
        String first = service.startFamily(userId);
        RefreshTokenService.Rotation second = service.rotate(first);

        assertThat(service.revokeFamilyOf(second.token())).contains(userId);

        assertThat(liveTokensOf(userId)).isZero();
        assertThat(service.revokeFamilyOf(Tokens.randomToken())).isEmpty();
    }

    @Test
    void twoFamiliesForOneUserAreIndependent() {
        UUID userId = UUID.randomUUID();
        String laptop = service.startFamily(userId);
        String phone = service.startFamily(userId);

        service.revokeFamilyOf(laptop);

        assertThat(service.rotate(phone).userId()).isEqualTo(userId);
    }
}
