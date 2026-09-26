package com.horizon.user;

import java.util.UUID;

/** What the auth module needs to check a login. */
public record UserCredentials(UUID userId, String phone, String passwordHash, boolean phoneVerified) {
}
