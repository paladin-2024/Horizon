package com.horizon.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues and checks the 6-digit SMS codes that verify a phone number. */
@Service
class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository repository;
    private final OtpHasher hasher;
    private final SmsSender smsSender;
    private final AuthProperties properties;

    OtpService(OtpCodeRepository repository, OtpHasher hasher, SmsSender smsSender,
            AuthProperties properties) {
        this.repository = repository;
        this.hasher = hasher;
        this.smsSender = smsSender;
        this.properties = properties;
    }

    /** Invalidates any code still outstanding for the phone, stores a new one and sends it. */
    @Transactional
    void issue(String phoneE164) {
        Instant now = Instant.now();
        repository.consumeAllFor(phoneE164, now);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        repository.save(new OtpCode(phoneE164, hasher.hash(phoneE164, code),
                OtpPurpose.PHONE_VERIFICATION, now.plus(properties.otpTtl()), now));
        long minutes = properties.otpTtl().toMinutes();
        smsSender.send(phoneE164,
                "Your Horizon verification code is " + code + ". It expires in " + minutes + " minutes.");
    }

    /**
     * @return true only if this is the outstanding code for the phone, unexpired, not yet used and
     *         not locked out; any false answer also burns one of the allowed attempts.
     */
    @Transactional
    boolean consume(String phoneE164, String code) {
        OtpCode active = repository.findFirstByPhoneAndConsumedAtIsNullOrderByIdDesc(phoneE164)
                .orElse(null);
        if (active == null) {
            return false;
        }
        Instant now = Instant.now();
        if (active.getExpiresAt().isBefore(now)) {
            active.consume(now);
            return false;
        }
        if (active.getAttempts() >= properties.otpMaxAttempts()) {
            active.consume(now);
            return false;
        }
        String expected = active.getCodeHash();
        String presented = code == null ? "" : hasher.hash(phoneE164, code);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8))) {
            active.recordFailedAttempt();
            if (active.getAttempts() >= properties.otpMaxAttempts()) {
                active.consume(now);
            }
            return false;
        }
        active.consume(now);
        return true;
    }
}
