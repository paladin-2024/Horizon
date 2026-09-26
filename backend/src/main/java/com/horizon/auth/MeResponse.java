package com.horizon.auth;

import com.horizon.user.UserView;
import java.util.UUID;

public record MeResponse(UUID id, String phone, String email, String firstName, String lastName,
        String country, String language) {

    static MeResponse from(UserView user) {
        return new MeResponse(user.id(), user.phone(), user.email(), user.firstName(),
                user.lastName(), user.country(), user.language());
    }
}
