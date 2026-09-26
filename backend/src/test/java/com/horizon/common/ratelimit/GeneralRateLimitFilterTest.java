package com.horizon.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.horizon.common.security.AuthenticatedUser;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GeneralRateLimitFilterTest {

    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(TimeMeter.SYSTEM_MILLISECONDS);
    private final GeneralRateLimitFilter filter =
            new GeneralRateLimitFilter(limiter, new RateLimitPolicy(2, Duration.ofMinutes(10)));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UUID userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId), null, List.of()));
    }

    private MockHttpServletResponse invoke(MockFilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void anonymousRequestsPassThrough() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = invoke(chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void authenticatedRequestsPassUntilTheBudgetIsSpent() throws Exception {
        authenticate(UUID.randomUUID());

        assertThat(invoke(new MockFilterChain()).getStatus()).isEqualTo(200);
        assertThat(invoke(new MockFilterChain()).getStatus()).isEqualTo(200);

        MockFilterChain blockedChain = new MockFilterChain();
        MockHttpServletResponse blocked = invoke(blockedChain);

        assertThat(blockedChain.getRequest()).isNull();
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(Long.parseLong(blocked.getHeader("Retry-After"))).isPositive();
        assertThat(blocked.getContentType()).startsWith("application/problem+json");
        assertThat(blocked.getContentAsString())
                .contains("\"code\":\"rate_limited\"")
                .contains("\"type\":\"urn:horizon:error:rate_limited\"")
                .contains("\"status\":429")
                .doesNotContain("about:blank");
    }

    @Test
    void budgetsAreIndependentPerUser() throws Exception {
        authenticate(UUID.randomUUID());
        invoke(new MockFilterChain());
        invoke(new MockFilterChain());
        assertThat(invoke(new MockFilterChain()).getStatus()).isEqualTo(429);

        authenticate(UUID.randomUUID());

        assertThat(invoke(new MockFilterChain()).getStatus()).isEqualTo(200);
    }
}
