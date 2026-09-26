package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SmsTestConfig.class)
class JwtServiceTest {

    @Autowired
    JwtService jwtService;

    private static String header(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
    }

    @Test
    void issuesAVerifiableTokenForTheUser() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueAccessToken(userId);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(header(token)).contains("\"alg\":\"HS256\"").contains("\"kid\":\"test\"");
        assertThat(jwtService.verifyAccessToken(token)).contains(userId);
    }

    @Test
    void rejectsAnExpiredToken() {
        String token = jwtService.issueAccessTokenAt(UUID.randomUUID(),
                Instant.now().minus(2, ChronoUnit.HOURS));

        assertThat(jwtService.verifyAccessToken(token)).isEmpty();
    }

    @Test
    void rejectsATamperedOrGarbageToken() {
        String token = jwtService.issueAccessToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("aa") ? "bb" : "aa");

        assertThat(jwtService.verifyAccessToken(tampered)).isEmpty();
        assertThat(jwtService.verifyAccessToken("not.a.token")).isEmpty();
        assertThat(jwtService.verifyAccessToken("")).isEmpty();
        assertThat(jwtService.verifyAccessToken(null)).isEmpty();
    }

    @Test
    void twoTokensForTheSameUserDiffer() {
        UUID userId = UUID.randomUUID();
        assertThat(jwtService.issueAccessToken(userId))
                .isNotEqualTo(jwtService.issueAccessToken(userId));
    }
}
