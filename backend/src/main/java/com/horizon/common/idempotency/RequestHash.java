package com.horizon.common.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

/**
 * The single request-hash rule (decision D2, shared with plan 05): lowercase hex SHA-256 over
 * method, request path (which carries path variables such as the account id), query string and
 * the content. Content is the cached body for an ordinary request. For a multipart request it is,
 * per file field in field-name order, the field name, the file name and a SHA-256 of the file
 * bytes read in 8 KiB chunks from {@link MultipartFile#getInputStream()}, so a 5 MB upload is never
 * held in memory (with {@code file-size-threshold=0} the container has already put it in a temp
 * file, and the handler can still read it afterwards). Text fields of a multipart request and the
 * parameters of a form-encoded request are hashed sorted by name. A body that was too large to
 * cache falls back to the content length.
 *
 * <p>Called from exactly one place, {@code IdempotencyInterceptor.preHandle}: by then the
 * DispatcherServlet has resolved the multipart request. Spring Security wraps the request, so the
 * multipart view is found with {@link WebUtils#getNativeRequest}, not with {@code instanceof}.
 */
final class RequestHash {

    private static final int CHUNK_BYTES = 8192;

    private RequestHash() {}

    static String of(HttpServletRequest request, byte[] cachedBody) {
        MessageDigest digest = sha256();
        update(digest, request.getMethod());
        update(digest, request.getRequestURI());
        update(digest, request.getQueryString() == null ? "" : request.getQueryString());
        MultipartHttpServletRequest multipart = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
        if (multipart != null) {
            updateMultipart(digest, multipart);
        } else if (cachedBody != null) {
            digest.update(cachedBody);
        } else if (isFormEncoded(request)) {
            updateParameters(digest, request.getParameterMap());
        } else {
            update(digest, "content-length:" + request.getContentLengthLong());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void updateMultipart(MessageDigest digest, MultipartHttpServletRequest request) {
        Map<String, List<MultipartFile>> files = new TreeMap<>(request.getMultiFileMap());
        for (Map.Entry<String, List<MultipartFile>> field : files.entrySet()) {
            for (MultipartFile file : field.getValue()) {
                update(digest, "file:" + field.getKey());
                update(digest, file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
                update(digest, streamedSha256(file));
            }
        }
        updateParameters(digest, request.getParameterMap());
    }

    private static void updateParameters(MessageDigest digest, Map<String, String[]> parameters) {
        for (Map.Entry<String, String[]> parameter : new TreeMap<>(parameters).entrySet()) {
            update(digest, "param:" + parameter.getKey());
            String[] values = parameter.getValue().clone();
            Arrays.sort(values);
            for (String value : values) {
                update(digest, value);
            }
        }
    }

    private static String streamedSha256(MultipartFile file) {
        MessageDigest fileDigest = sha256();
        byte[] chunk = new byte[CHUNK_BYTES];
        try (InputStream in = file.getInputStream()) {
            int read;
            while ((read = in.read(chunk)) != -1) {
                fileDigest.update(chunk, 0, read);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file to hash the request", e);
        }
        return HexFormat.of().formatHex(fileDigest.digest());
    }

    private static boolean isFormEncoded(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded");
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is always available", impossible);
        }
    }
}
