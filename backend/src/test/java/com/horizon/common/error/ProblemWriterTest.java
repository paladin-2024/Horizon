package com.horizon.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

class ProblemWriterTest {

    @Test
    void writesTheSameShapeAsTheExceptionHandler() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ProblemWriter.write(response, HttpStatus.FORBIDDEN, "csrf_header_required", "Missing X-Horizon-Client");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"type\":\"urn:horizon:error:csrf_header_required\"")
                .contains("\"title\":\"Forbidden\"")
                .contains("\"status\":403")
                .contains("\"detail\":\"Missing X-Horizon-Client\"")
                .contains("\"code\":\"csrf_header_required\"");
    }

    @Test
    void neverWritesAboutBlankAsTheType() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ProblemWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "Slow down");

        assertThat(response.getContentAsString()).doesNotContain("about:blank");
    }

    @Test
    void addsTheGivenHeadersAndEscapesTheDetail() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ProblemWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                "Retry after \"30\" seconds", Map.of("Retry-After", "30"));

        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentAsString()).contains("Retry after \\\"30\\\" seconds");
    }
}
