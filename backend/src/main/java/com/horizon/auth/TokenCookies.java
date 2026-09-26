package com.horizon.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds, sends and clears the two auth cookies. */
@Component
class TokenCookies {

    static final String ACCESS_COOKIE = "hz_access";
    static final String REFRESH_COOKIE = "hz_refresh";
    static final String ACCESS_PATH = "/";
    static final String REFRESH_PATH = "/api/v1/auth";

    private final AuthProperties properties;

    TokenCookies(AuthProperties properties) {
        this.properties = properties;
    }

    void write(HttpServletRequest request, HttpServletResponse response, String accessToken,
            String refreshToken) {
        boolean secure = request.isSecure();
        // Deliberately the refresh lifetime, not accessTokenTtl: the cookie must outlive the 15-minute JWT
        // inside it, so the browser keeps sending it and the server can answer 401 (stale) instead of
        // the cookie silently vanishing. The JWT's own exp is what limits its validity.
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(ACCESS_COOKIE, accessToken, ACCESS_PATH, properties.refreshTokenTtl(), secure));
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(REFRESH_COOKIE, refreshToken, REFRESH_PATH, properties.refreshTokenTtl(),
                        secure));
    }

    void clear(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = request.isSecure();
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(ACCESS_COOKIE, "", ACCESS_PATH, Duration.ZERO, secure));
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(REFRESH_COOKIE, "", REFRESH_PATH, Duration.ZERO, secure));
    }

    static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private static String cookie(String name, String value, String path, Duration maxAge,
            boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
