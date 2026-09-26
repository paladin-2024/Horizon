package com.horizon.user;

/**
 * A user to create. {@code passwordHash} is already hashed by the caller (the auth module owns
 * password hashing); {@code country} is "UG" or "CD" and {@code language} is "en" or "fr".
 */
public record NewUser(String phone, String passwordHash, String firstName, String lastName,
        String country, String language, String email, String nationalId) {
}
