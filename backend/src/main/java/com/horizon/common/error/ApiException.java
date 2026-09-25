package com.horizon.common.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * An error the client caused or can act on. {@link GlobalExceptionHandler} turns it into an
 * RFC 7807 response whose {@code code} property is the stable, machine-readable {@link #getCode()}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final HttpHeaders headers;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, HttpHeaders.EMPTY);
    }

    /** Use the headers overload for responses that need extra headers, such as {@code Retry-After}. */
    public ApiException(HttpStatus status, String code, String message, HttpHeaders headers) {
        super(message);
        this.status = status;
        this.code = code;
        this.headers = headers;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException unprocessable(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
