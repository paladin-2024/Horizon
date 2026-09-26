package com.horizon.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 of "phone:code" with a server-side pepper. A bare hash of a 6-digit code is
 * brute-forceable from a database dump in milliseconds; the pepper is not in the database.
 */
@Component
class OtpHasher {

    private final byte[] pepper;

    OtpHasher(AuthProperties properties) {
        this.pepper = Base64.getDecoder().decode(properties.otpPepper());
        if (this.pepper.length < 32) {
            throw new IllegalStateException("horizon.auth.otp-pepper must decode to at least 32 bytes");
        }
    }

    String hash(String phone, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            byte[] digest = mac.doFinal((phone + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not hash the OTP", e);
        }
    }
}
