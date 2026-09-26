package com.horizon.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small real-HTTP client for the tests: no cookie jar, so every test states what it sends. */
class HttpApi {

    private final HttpClient client = HttpClient.newHttpClient();
    private final String base;

    HttpApi(int port) {
        this.base = "http://localhost:" + port;
    }

    private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

    /**
     * A client IP no other request in this JVM has used (10.x.y.z, first-hop form). The test profile
     * trusts X-Forwarded-For, so each call looks like a different browser and the per-IP limits
     * (login 5 per minute, OTP 10 per hour) never cross tests.
     */
    static String freshIp() {
        int n = NEXT_IP.getAndIncrement();
        return "10." + ((n >> 16) & 255) + "." + ((n >> 8) & 255) + "." + (n & 255);
    }

    static Map<String, String> headers(String... keyValuePairs) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            headers.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return headers;
    }

    HttpResponse<String> get(String path, Map<String, String> headers) {
        return send(request(path, headers).GET());
    }

    /** POST as the web client does: with the CSRF header, a fresh idempotency key and a fresh client IP. */
    HttpResponse<String> post(String path, String json, Map<String, String> headers) {
        Map<String, String> all = new LinkedHashMap<>();
        all.put(CsrfHeaderFilter.HEADER, CsrfHeaderFilter.EXPECTED);
        all.put("Idempotency-Key", UUID.randomUUID().toString());
        all.put("X-Forwarded-For", freshIp());
        all.putAll(headers);
        return postRaw(path, json, all);
    }

    /** POST with exactly the headers given (plus Content-Type). */
    HttpResponse<String> postRaw(String path, String json, Map<String, String> headers) {
        return send(request(path, headers)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    private HttpRequest.Builder request(String path, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path));
        headers.forEach(builder::header);
        return builder;
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) {
        try {
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("HTTP call failed", e);
        }
    }

    /** The whole Set-Cookie line for {@code name}, attributes included. */
    static Optional<String> setCookie(HttpResponse<?> response, String name) {
        return response.headers().allValues("set-cookie").stream()
                .filter(value -> value.startsWith(name + "="))
                .reduce((first, second) -> second);
    }

    /** Just the value of the cookie {@code name}. */
    static Optional<String> cookieValue(HttpResponse<?> response, String name) {
        return setCookie(response, name)
                .map(line -> line.substring(name.length() + 1).split(";", 2)[0]);
    }

    static String cookieHeader(String... nameThenValue) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < nameThenValue.length; i += 2) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(nameThenValue[i]).append('=').append(nameThenValue[i + 1]);
        }
        return builder.toString();
    }

    /** Reads a top-level string field out of a JSON body without pulling in a JSON parser. */
    static String jsonString(String body, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("No string field '" + field + "' in: " + body);
        }
        return matcher.group(1);
    }
}
