package com.horizon.common.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The authenticated user id, or empty when the request is anonymous. {@link CurrentUser#id()}
 * throws in that case; idempotency and rate limiting have to treat anonymous requests as a normal,
 * unauthenticated scope instead.
 */
public final class OptionalCurrentUser {

    private OptionalCurrentUser() {}

    public static Optional<UUID> userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user.userId());
        }
        return Optional.empty();
    }
}
