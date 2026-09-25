package com.horizon.common.error;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error response is an RFC 7807 {@link ProblemDetail} with an extra string property
 * {@code code}. Spring MVC's own exceptions (404, 405, 415, malformed body, missing parameter)
 * are handled by the base class and get a {@code code} derived from the HTTP status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "urn:horizon:error:";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex) {
        ProblemDetail problem = problem(ex.getStatus(), ex.getCode(), ex.getMessage());
        if (ex.getStatus().is5xxServerError()) {
            log.error("Server error {}: {}", ex.getCode(), ex.getMessage(), ex);
        }
        return ResponseEntity.status(ex.getStatus()).headers(ex.getHeaders()).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthentication(AuthenticationException ex) {
        return handleApiException(ApiException.unauthorized("unauthenticated", "Authentication is required"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return handleApiException(ApiException.forbidden("forbidden", "You do not have access to this resource"));
    }

    /**
     * Two writers touched the same row at once. This is a normal, retryable client outcome, not a
     * server fault, so it must never surface as 500. {@link ObjectOptimisticLockingFailureException}
     * (what Hibernate's {@code @Version} check throws through Spring Data) is a subclass of
     * {@link OptimisticLockingFailureException}, so one handler covers both.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return handleApiException(ApiException.conflict(
                "concurrent_update", "This record changed while your request was in flight; retry it"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return handleApiException(new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Something went wrong on our side"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldIssue> issues = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> issues.add(new FieldIssue(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> issues.add(new FieldIssue(e.getObjectName(), e.getDefaultMessage())));
        return validationFailed(issues);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldIssue> issues = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors errors) {
                errors.getFieldErrors()
                        .forEach(e -> issues.add(new FieldIssue(e.getField(), e.getDefaultMessage())));
                errors.getGlobalErrors()
                        .forEach(e -> issues.add(new FieldIssue(e.getObjectName(), e.getDefaultMessage())));
            } else {
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors()
                        .forEach(e -> issues.add(new FieldIssue(name, e.getDefaultMessage())));
            }
        });
        return validationFailed(issues);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            String code = codeFor(statusCode);
            problem.setProperty("code", code);
            problem.setType(URI.create(TYPE_PREFIX + code));
        }
        return response;
    }

    private ResponseEntity<Object> validationFailed(List<FieldIssue> issues) {
        issues.sort(Comparator.comparing(FieldIssue::field, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(FieldIssue::message, Comparator.nullsFirst(Comparator.naturalOrder())));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed");
        problem.setProperty("errors", issues);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatusCode status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setProperty("code", code);
        return problem;
    }

    private static String codeFor(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.name().toLowerCase(Locale.ROOT) : "http_" + status.value();
    }
}
