package com.horizon.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.horizon.support.IntegrationTest;
import com.horizon.support.ProbeController;
import com.horizon.support.TestAuth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import(ProbeController.class)
@ExtendWith(OutputCaptureExtension.class)
class RequestIdFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void generatesAnIdWhenTheCallerSendsNone() throws Exception {
        String id = mockMvc.perform(get("/test-support/request-id").with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(UUID.fromString(id)).isNotNull();
    }

    @Test
    void echoesACallerSuppliedIdAndExposesItInTheMdc() throws Exception {
        mockMvc.perform(get("/test-support/request-id")
                        .header("X-Request-Id", "client-req-123")
                        .with(TestAuth.asUser(UUID.randomUUID())))
                .andExpect(header().string("X-Request-Id", "client-req-123"))
                .andExpect(jsonPath("$.requestId").value("client-req-123"));
    }

    @Test
    void replacesAnUnsafeCallerSuppliedId() throws Exception {
        String id = mockMvc.perform(get("/test-support/request-id")
                        .header("X-Request-Id", "bad id with spaces")
                        .with(TestAuth.asUser(UUID.randomUUID())))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(id).isNotEqualTo("bad id with spaces");
        assertThat(UUID.fromString(id)).isNotNull();
    }

    @Test
    void replacesATooLongCallerSuppliedId() throws Exception {
        String tooLong = "a".repeat(65);

        String id = mockMvc.perform(get("/test-support/request-id")
                        .header("X-Request-Id", tooLong)
                        .with(TestAuth.asUser(UUID.randomUUID())))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(id).isNotEqualTo(tooLong);
    }

    @Test
    void unauthenticatedResponsesCarryTheHeaderToo() throws Exception {
        mockMvc.perform(get("/test-support/request-id").header("X-Request-Id", "anon-req-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "anon-req-1"));
    }

    @Test
    void clearsTheMdcWhenTheRequestEnds() throws Exception {
        mockMvc.perform(get("/test-support/request-id").with(TestAuth.asUser(UUID.randomUUID())));

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void consoleLogsAreJsonAndCarryTheRequestId(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test-support/request-id")
                .header("X-Request-Id", "log-req-42")
                .with(TestAuth.asUser(UUID.randomUUID())));

        assertThat(output.getOut())
                .contains("\"message\":\"probe request-id endpoint called\"")
                .contains("\"requestId\":\"log-req-42\"");
    }
}
