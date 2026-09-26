package com.horizon.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank String phone,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String code) {
}
