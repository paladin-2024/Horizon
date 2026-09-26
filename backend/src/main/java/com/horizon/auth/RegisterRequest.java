package com.horizon.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String phone,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Pattern(regexp = "UG|CD", message = "must be UG or CD") String country,
        @Email @Size(max = 255) String email,
        @Size(max = 64) String nationalId) {
}
