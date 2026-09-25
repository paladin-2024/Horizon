package com.horizon.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.horizon.support.IntegrationTest;
import com.horizon.support.ProbeController;
import com.horizon.support.TestAuth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@Import(ProbeController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    private ResultActions getAsUser(String path) throws Exception {
        return mockMvc.perform(get(path).with(TestAuth.asUser(UUID.randomUUID())));
    }

    private ResultActions postJsonAsUser(String path, String json) throws Exception {
        return mockMvc.perform(post(path)
                .with(TestAuth.asUser(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void apiExceptionBecomesAProblemDetailWithCode() throws Exception {
        getAsUser("/test-support/api-error?status=404&code=account_not_found")
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:horizon:error:account_not_found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("probe message for account_not_found"))
                .andExpect(jsonPath("$.code").value("account_not_found"));
    }

    @ParameterizedTest
    @CsvSource({"400,bad_thing", "401,no_auth", "403,not_yours", "404,missing", "409,clash", "422,unprocessable_thing"})
    void everyClientStatusKeepsItsStatusAndCode(int status, String code) throws Exception {
        getAsUser("/test-support/api-error?status=" + status + "&code=" + code)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void beanValidationFailureListsEveryInvalidField() throws Exception {
        postJsonAsUser("/test-support/validate", "{\"name\":\"\",\"quantity\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[1].field").value("quantity"))
                .andExpect(jsonPath("$.errors[1].message").value("must be greater than or equal to 1"));
    }

    @Test
    void validRequestPassesValidation() throws Exception {
        postJsonAsUser("/test-support/validate", "{\"name\":\"ok\",\"quantity\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ok"));
    }

    @ParameterizedTest
    @CsvSource({"0", "101"})
    void constraintOnARequestParameterIsAValidationFailure(int limit) throws Exception {
        getAsUser("/test-support/limit?limit=" + limit)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }

    @Test
    void malformedJsonIsABadRequestWithCode() throws Exception {
        postJsonAsUser("/test-support/validate", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"));
    }

    @Test
    void unparseableParameterIsABadRequestWithCode() throws Exception {
        getAsUser("/test-support/limit?limit=abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"));
    }

    @Test
    void wrongMethodIs405WithCode() throws Exception {
        mockMvc.perform(post("/test-support/whoami").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("method_not_allowed"));
    }

    @Test
    void wrongContentTypeIs415WithCode() throws Exception {
        mockMvc.perform(post("/test-support/validate")
                        .with(TestAuth.asUser(UUID.randomUUID()))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("unsupported_media_type"));
    }

    @Test
    void unknownPathIs404WithCode() throws Exception {
        getAsUser("/test-support/no-such-endpoint")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void unexpectedExceptionIs500WithoutLeakingDetails() throws Exception {
        getAsUser("/test-support/boom")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal_error"))
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret internal detail"))));
    }

    @Test
    void apiExceptionHeadersAreCopiedToTheResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "30");
        ApiException e = new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "Slow down", headers);

        ResponseEntity<Object> response = new GlobalExceptionHandler().handleApiException(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("30");
    }

    @Test
    void mapsAnOptimisticLockFailureTo409ConcurrentUpdate() throws Exception {
        mockMvc.perform(get("/test-support/stale").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("concurrent_update"))
                .andExpect(jsonPath("$.type").value("urn:horizon:error:concurrent_update"));
    }

    @Test
    void mapsHibernatesObjectOptimisticLockSubclassTheSameWay() throws Exception {
        mockMvc.perform(get("/test-support/stale?kind=object").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("concurrent_update"));
    }
}
