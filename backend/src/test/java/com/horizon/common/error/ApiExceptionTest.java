package com.horizon.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {

    @Test
    void constructorKeepsStatusCodeAndMessage() {
        ApiException e = new ApiException(HttpStatus.I_AM_A_TEAPOT, "teapot", "short and stout");

        assertThat(e.getStatus()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(e.getCode()).isEqualTo("teapot");
        assertThat(e.getMessage()).isEqualTo("short and stout");
        assertThat(e.getHeaders().isEmpty()).isTrue();
    }

    @Test
    void factoriesUseTheMatchingStatus() {
        assertThat(ApiException.badRequest("c", "m").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ApiException.unauthorized("c", "m").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ApiException.forbidden("c", "m").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ApiException.notFound("c", "m").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ApiException.conflict("c", "m").getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ApiException.unprocessable("c", "m").getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void factoriesKeepCodeAndMessage() {
        ApiException e = ApiException.notFound("account_not_found", "No such account");

        assertThat(e.getCode()).isEqualTo("account_not_found");
        assertThat(e.getMessage()).isEqualTo("No such account");
    }

    @Test
    void headersOverloadCarriesExtraHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "30");

        ApiException e = new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "Slow down", headers);

        assertThat(e.getHeaders().getFirst("Retry-After")).isEqualTo("30");
    }
}
