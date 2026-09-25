package com.horizon.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.horizon.common.security.AuthenticatedUser;
import com.horizon.support.IdempotencyTestController;
import com.horizon.support.IntegrationTest;
import com.horizon.support.ResilienceTestSecurity;
import com.horizon.support.TestAuth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@Import({ResilienceTestSecurity.class, IdempotencyTestController.class})
class IdempotencyWebTest {

    private static final String BODY = "{\"value\":\"hello\",\"delayMs\":0,\"fail\":false}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    IdempotencyTestController controller;

    @Autowired
    IdempotencyKeyRepository repository;

    @BeforeEach
    void resetCounter() {
        controller.reset();
    }

    private MockHttpServletRequestBuilder echo(String key, String body, UUID userId) {
        MockHttpServletRequestBuilder request = post("/api/v1/test/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Horizon-Client", "web")
                .content(body);
        if (key != null) {
            request = request.header("Idempotency-Key", key);
        }
        return userId == null ? request : request.with(TestAuth.asUser(userId));
    }

    /**
     * No {@code csrf()} here (unlike {@link TestAuth#asUser}): the test chain disables CSRF, and
     * MockMvc's {@code csrf()} postprocessor mints a fresh random {@code _csrf} form field on every
     * call since there is no session to reuse a token from. {@link RequestHash} legitimately hashes
     * every multipart form field, so that fresh field would make two calls with the same file hash
     * differently and never replay.
     */
    private RequestBuilder upload(String key, String filename, byte[] content, UUID userId) {
        return multipart("/api/v1/test/upload")
                .file(new MockMultipartFile("file", filename, "text/csv", content))
                .header("X-Horizon-Client", "web")
                .header("Idempotency-Key", key)
                .with(authentication(
                        UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedUser(userId), null, List.of())));
    }

    @Test
    void aMissingHeaderIsRejected() throws Exception {
        mockMvc.perform(echo(null, BODY, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("idempotency_key_required"))
                .andExpect(jsonPath("$.type").value("urn:horizon:error:idempotency_key_required"));
        assertThat(controller.invocations()).isZero();
    }

    @Test
    void anOverlongHeaderIsRejected() throws Exception {
        mockMvc.perform(echo("k".repeat(201), BODY, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("idempotency_key_invalid"));
        assertThat(controller.invocations()).isZero();
    }

    @Test
    void theSameKeyAndBodyReplaysTheStoredResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();

        MvcResult first = mockMvc.perform(echo(key, BODY, userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invocation").value(1))
                .andReturn();

        MvcResult replay = mockMvc.perform(echo(key, BODY, userId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn();

        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertThat(controller.invocations()).isEqualTo(1);
    }

    @Test
    void theSameKeyWithADifferentBodyIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        mockMvc.perform(echo(key, BODY, userId)).andExpect(status().isCreated());

        mockMvc.perform(echo(key, "{\"value\":\"other\",\"delayMs\":0,\"fail\":false}", userId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"))
                .andExpect(jsonPath("$.type").value("urn:horizon:error:idempotency_key_reused"));

        assertThat(controller.invocations()).isEqualTo(1);
    }

    @Test
    void keysAreScopedPerUser() throws Exception {
        String key = UUID.randomUUID().toString();

        mockMvc.perform(echo(key, BODY, UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invocation").value(1));
        mockMvc.perform(echo(key, BODY, UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invocation").value(2));
    }

    @Test
    void aFailedRequestReleasesTheKey() throws Exception {
        UUID userId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();

        mockMvc.perform(echo(key, "{\"value\":\"boom\",\"delayMs\":0,\"fail\":true}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("test_failure"));

        assertThat(repository.findByScopeIdAndKey(userId, key)).isEmpty();

        mockMvc.perform(echo(key, BODY, userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invocation").value(2));
    }

    @Test
    void anonymousRequestsShareTheAnonymousScope() throws Exception {
        String key = UUID.randomUUID().toString();

        mockMvc.perform(echo(key, BODY, null)).andExpect(status().isCreated());
        mockMvc.perform(echo(key, BODY, null))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        assertThat(controller.invocations()).isEqualTo(1);
        assertThat(repository.findByScopeIdAndKey(IdempotencyKey.ANONYMOUS_SCOPE, key)).isPresent();
    }

    @Test
    void theSameFileWithTheSameKeyReplays() throws Exception {
        UUID userId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        byte[] statement = "date,description,amount\n2026-03-04,Airtel,-1000\n".getBytes();

        mockMvc.perform(upload(key, "march.csv", statement, userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invocation").value(1));
        mockMvc.perform(upload(key, "march.csv", statement, userId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.invocation").value(1));

        assertThat(controller.invocations()).isEqualTo(1);
    }

    @Test
    void aDifferentFileOfTheSameLengthWithTheSameKeyIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();

        mockMvc.perform(upload(key, "march.csv", "date,description,amount\n2026-03-04,Airtel,-1000\n".getBytes(), userId))
                .andExpect(status().isCreated());
        mockMvc.perform(upload(key, "march.csv", "date,description,amount\n2026-03-04,Airtel,-2000\n".getBytes(), userId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));

        assertThat(controller.invocations()).isEqualTo(1);
    }
}
