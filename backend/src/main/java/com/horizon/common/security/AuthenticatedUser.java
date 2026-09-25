package com.horizon.common.security;

import java.util.Objects;
import java.util.UUID;

/** The principal placed in the SecurityContext once a request is authenticated. */
public record AuthenticatedUser(UUID userId) {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
    }
}
