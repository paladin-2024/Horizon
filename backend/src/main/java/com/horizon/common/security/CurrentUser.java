package com.horizon.common.security;

import com.horizon.common.error.ApiException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Access to the authenticated user of the current request. */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The authenticated user's id, or a 401 {@code unauthenticated} error if there is none. */
    public static UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        throw ApiException.unauthorized("unauthenticated", "Authentication is required");
    }
}
