package com.horizon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpTest {

    private static MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void ignoresXForwardedForWhenNotTrusted() {
        assertThat(new ClientIp(false).of(request("203.0.113.9"))).isEqualTo("192.0.2.10");
    }

    @Test
    void usesTheFirstHopWhenTrusted() {
        assertThat(new ClientIp(true).of(request("203.0.113.9, 198.51.100.1"))).isEqualTo("203.0.113.9");
    }

    @Test
    void trimsWhitespaceAroundTheFirstHop() {
        assertThat(new ClientIp(true).of(request("  203.0.113.9  ,198.51.100.1"))).isEqualTo("203.0.113.9");
    }

    @Test
    void fallsBackToTheSocketAddressWhenTrustedButTheHeaderIsMissingOrBlank() {
        assertThat(new ClientIp(true).of(request(null))).isEqualTo("192.0.2.10");
        assertThat(new ClientIp(true).of(request("   "))).isEqualTo("192.0.2.10");
    }
}
