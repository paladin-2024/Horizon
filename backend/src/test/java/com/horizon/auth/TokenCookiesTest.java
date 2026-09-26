package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TokenCookiesTest {

    private final AuthProperties properties = new AuthProperties("unused", "test", "horizon",
            Duration.ofMinutes(15), Duration.ofDays(30), Duration.ofMinutes(5), 5, "unused");
    private final TokenCookies cookies = new TokenCookies(properties);

    private static String cookie(MockHttpServletResponse response, String name) {
        return response.getHeaders("Set-Cookie").stream()
                .filter(line -> line.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theAccessCookieOutlivesTheJwtInsideIt() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.write(new MockHttpServletRequest(), response, "jwt", "refresh");

        long thirtyDays = Duration.ofDays(30).toSeconds();
        assertThat(cookie(response, "hz_access")).contains("Max-Age=" + thirtyDays);
        assertThat(cookie(response, "hz_refresh")).contains("Max-Age=" + thirtyDays);
        assertThat(properties.accessTokenTtl()).isLessThan(properties.refreshTokenTtl());
    }

    @Test
    void bothCookiesAreHttpOnlySameSiteStrictAndScopedAsSpecified() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.write(request, response, "jwt", "refresh");

        assertThat(cookie(response, "hz_access")).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/");
        assertThat(cookie(response, "hz_refresh"))
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");
    }

    @Test
    void clearingExpiresBothCookiesImmediately() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookies.clear(new MockHttpServletRequest(), response);

        assertThat(cookie(response, "hz_access")).contains("Max-Age=0");
        assertThat(cookie(response, "hz_refresh")).contains("Max-Age=0").contains("Path=/api/v1/auth");
    }
}
