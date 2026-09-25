package com.horizon.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.horizon.support.IntegrationTest;
import com.horizon.support.RateLimitTestController;
import com.horizon.support.ResilienceTestSecurity;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import({ResilienceTestSecurity.class, RateLimitTestController.class})
class RateLimitGuardTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RateLimitGuard guard;

    @Test
    void throwsWhenTheBucketIsEmpty() {
        String bucket = "unit:" + UUID.randomUUID();
        RateLimitPolicy policy = new RateLimitPolicy(2, Duration.ofMinutes(10));

        guard.check(bucket, policy);
        guard.check(bucket, policy);

        assertThatThrownBy(() -> guard.check(bucket, policy))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isPositive());
    }

    @Test
    void overTheLimitReturns429WithRetryAfterAndProblemCode() throws Exception {
        String bucket = "http:" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/test/rate-limited").param("bucket", bucket)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/test/rate-limited").param("bucket", bucket)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/test/rate-limited").param("bucket", bucket))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("rate_limited"))
                .andExpect(jsonPath("$.type").value("urn:horizon:error:rate_limited"));
    }
}
