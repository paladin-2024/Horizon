package com.horizon.user;

import java.util.UUID;

/** The user data other modules and the API are allowed to see. */
public record UserView(UUID id, String phone, String email, String firstName, String lastName,
        String country, String language, boolean phoneVerified) {
}
