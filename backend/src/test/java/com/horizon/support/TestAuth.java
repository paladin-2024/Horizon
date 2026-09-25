package com.horizon.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.horizon.common.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Authenticates a MockMvc request as a given user without going through the auth module.
 * It installs an {@link AuthenticatedUser} principal and also satisfies the CSRF rules
 * (a Spring CSRF token if a chain ever enables one, the {@code X-Horizon-Client: web} header once the auth
 * slice lands), so it works for POST, PUT, PATCH and DELETE requests as well.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor asUser(UUID userId) {
        RequestPostProcessor authenticated = authentication(
                UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedUser(userId), null, List.of()));
        RequestPostProcessor csrfToken = csrf();
        return request -> {
            request = authenticated.postProcessRequest(request);
            request = csrfToken.postProcessRequest(request);
            request.addHeader("X-Horizon-Client", "web");
            return request;
        };
    }
}
