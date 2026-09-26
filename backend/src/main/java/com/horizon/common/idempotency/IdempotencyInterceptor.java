package com.horizon.common.idempotency;

import com.horizon.common.error.ApiException;
import com.horizon.common.security.OptionalCurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

/**
 * Claims the key before the handler runs and stores (or releases) the result afterwards.
 * {@code preHandle} exceptions are resolved by plan 01's {@code GlobalExceptionHandler}, so the
 * 400 / 409 / 422 answers keep the RFC 7807 shape with {@code type = urn:horizon:error:<code>}
 * and this class never hand-writes an error body (only the stored success response is replayed).
 * The request hash is computed here and nowhere else, after the DispatcherServlet has resolved a
 * multipart request (see {@link RequestHash}).
 */
@Component
class IdempotencyInterceptor implements HandlerInterceptor {

    static final String HEADER = "Idempotency-Key";
    static final String REPLAYED_HEADER = "Idempotency-Replayed";
    static final int MAX_KEY_LENGTH = 200;

    private static final String RECORD_ID_ATTRIBUTE = IdempotencyInterceptor.class.getName() + ".recordId";

    private final IdempotencyService service;

    IdempotencyInterceptor(IdempotencyService service) {
        this.service = service;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)
                || handlerMethod.getMethodAnnotation(Idempotent.class) == null
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("idempotency_key_required", "The Idempotency-Key header is required.");
        }
        key = key.trim();
        if (key.length() > MAX_KEY_LENGTH) {
            throw ApiException.badRequest(
                    "idempotency_key_invalid", "The Idempotency-Key header must be at most 200 characters.");
        }
        UUID userId = OptionalCurrentUser.userId().orElse(null);
        byte[] cachedBody = (byte[]) request.getAttribute(CachedBodyRequestFilter.BODY_ATTRIBUTE);
        IdempotencyOutcome outcome = service.begin(userId, key, RequestHash.of(request, cachedBody));
        if (outcome instanceof IdempotencyOutcome.Replay replay) {
            writeReplay(response, replay);
            return false;
        }
        request.setAttribute(RECORD_ID_ATTRIBUTE, ((IdempotencyOutcome.Proceed) outcome).recordId());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception failure) {
        Object recordId = request.getAttribute(RECORD_ID_ATTRIBUTE);
        if (recordId == null) {
            return;
        }
        request.removeAttribute(RECORD_ID_ATTRIBUTE);
        int status = response.getStatus();
        if (failure != null || status < 200 || status >= 300) {
            service.release((UUID) recordId);
            return;
        }
        byte[] body = capturedBody(response);
        service.complete((UUID) recordId, status, body.length == 0 ? null : new String(body, StandardCharsets.UTF_8));
    }

    private void writeReplay(HttpServletResponse response, IdempotencyOutcome.Replay replay) throws IOException {
        response.setStatus(replay.status());
        response.setHeader(REPLAYED_HEADER, "true");
        if (replay.body() != null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getOutputStream().write(replay.body().getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] capturedBody(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        return wrapper == null ? new byte[0] : wrapper.getContentAsByteArray();
    }
}
