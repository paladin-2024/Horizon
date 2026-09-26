package com.horizon.common.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * On POST requests: keeps the request body so it can be hashed and still read by the controller,
 * and wraps the response so the interceptor can store what was sent.
 */
public class CachedBodyRequestFilter extends OncePerRequestFilter {

    static final String BODY_ATTRIBUTE = CachedBodyRequestFilter.class.getName() + ".body";

    /** Bodies larger than this are not cached; their hash falls back to the content length. */
    static final long MAX_CACHED_BYTES = 1024L * 1024L;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest requestToUse = request;
        if (isCacheable(request)) {
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
            request.setAttribute(BODY_ATTRIBUTE, body);
            requestToUse = new CachedBodyHttpServletRequest(request, body);
        }
        ContentCachingResponseWrapper responseToUse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(requestToUse, responseToUse);
        } finally {
            responseToUse.copyBodyToResponse();
        }
    }

    private boolean isCacheable(HttpServletRequest request) {
        if (request.getContentLengthLong() > MAX_CACHED_BYTES) {
            return false;
        }
        String contentType = request.getContentType();
        if (contentType == null) {
            return true;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return !normalized.startsWith("multipart/") && !normalized.startsWith("application/x-www-form-urlencoded");
    }
}
