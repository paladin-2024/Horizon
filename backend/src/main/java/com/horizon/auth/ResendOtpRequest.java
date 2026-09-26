package com.horizon.auth;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(@NotBlank String phone) {
}
