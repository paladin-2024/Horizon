package com.horizon.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.horizon.common.error.ProblemWriter;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cookie authentication needs a CSRF defense. A cross-site form or image cannot set a custom
 * header, so every state-changing request must carry {@code X-Horizon-Client: web}.
 *
 * <p>This runs before authorization, outside the {@code @RestControllerAdvice}, so it answers
 * through {@link ProblemWriter} (plan 01), the single writer that keeps the problem body identical
 * to {@code GlobalExceptionHandler}'s, including {@code type = urn:horizon:error:<code>}.
 */
@Component
public class CsrfHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Horizon-Client";
    public static final String EXPECTED = "web";

    private static final Set<String> STATE_CHANGING = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (STATE_CHANGING.contains(request.getMethod())
                && !EXPECTED.equals(request.getHeader(HEADER))) {
            ProblemWriter.write(response, HttpStatus.FORBIDDEN, "csrf_header_required",
                    "State-changing requests must send the X-Horizon-Client: web header.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
