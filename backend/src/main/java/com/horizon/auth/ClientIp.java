package com.horizon.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The client address used as the key of the per-IP limits ({@code LOGIN_PER_IP},
 * {@code OTP_SEND_PER_IP}). In every planned environment the API is reached through the Next.js
 * proxy, so the socket address is the proxy and the real client is the first hop of
 * {@code X-Forwarded-For}. But while the API port is directly reachable a client can send that
 * header itself, so it is trusted only when {@code horizon.security.trust-forwarded-for} is true
 * (default false; true in local development and in the test profile). Otherwise the socket address
 * is used. {@code server.forward-headers-strategy} deliberately stays unset.
 */
@Component
class ClientIp {

    private final boolean trustForwardedFor;

    ClientIp(@Value("${horizon.security.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    String of(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String firstHop = forwarded.split(",")[0].trim();
                if (!firstHop.isEmpty()) {
                    return firstHop;
                }
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
