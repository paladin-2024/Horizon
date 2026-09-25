package com.horizon.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import(ProbeController.class)
class TestAuthTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void asUserMakesTheUserIdAvailableThroughCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/test-support/whoami").with(TestAuth.asUser(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void differentRequestsCanBeDifferentUsers() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        mockMvc.perform(get("/test-support/whoami").with(TestAuth.asUser(first)))
                .andExpect(jsonPath("$.userId").value(first.toString()));
        mockMvc.perform(get("/test-support/whoami").with(TestAuth.asUser(second)))
                .andExpect(jsonPath("$.userId").value(second.toString()));
    }

    @Test
    void requestsWithoutTestAuthAreRejected() throws Exception {
        mockMvc.perform(get("/test-support/whoami")).andExpect(status().isUnauthorized());
    }

    @Test
    void asUserAlsoWorksForStateChangingRequests() throws Exception {
        mockMvc.perform(post("/test-support/echo").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}
