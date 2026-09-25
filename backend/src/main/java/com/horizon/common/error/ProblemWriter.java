package com.horizon.common.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * The single way a servlet filter or handler interceptor writes an error body. Those run outside
 * {@link GlobalExceptionHandler}, so without this helper each one would hand-write JSON and the
 * shapes would drift (the usual drift is {@code "type":"about:blank"}). The body written here is
 * the same shape the handler produces, including {@code urn:horizon:error:<code>} as the type.
 *
 * <p>Jackson is deliberately not used: a filter may run before or after the message converters are
 * usable, and this body has five known fields.
 */
public final class ProblemWriter {

    private ProblemWriter() {}

    public static void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        write(response, status, code, detail, Map.of());
    }

    public static void write(HttpServletResponse response, HttpStatus status, String code, String detail,
            Map<String, String> headers) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        headers.forEach(response::setHeader);
        String body = "{\"type\":\"urn:horizon:error:" + escape(code) + "\""
                + ",\"title\":\"" + escape(status.getReasonPhrase()) + "\""
                + ",\"status\":" + status.value()
                + ",\"detail\":\"" + escape(detail) + "\""
                + ",\"code\":\"" + escape(code) + "\"}";
        response.getWriter().write(body);
        response.getWriter().flush();
    }

    private static String escape(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
