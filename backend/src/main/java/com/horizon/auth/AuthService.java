package com.horizon.auth;

import com.horizon.common.audit.AuditLog;
import com.horizon.common.error.ApiException;
import com.horizon.common.ratelimit.RateLimitGuard;
import com.horizon.common.ratelimit.RateLimitPolicies;
import com.horizon.user.NewUser;
import com.horizon.user.UserCredentials;
import com.horizon.user.UserService;
import com.horizon.user.UserView;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** The auth flows. Every answer is written so it cannot be used to discover who has an account. */
@Service
class AuthService {

    /** What the controller needs to answer a successful sign-in. */
    record Session(MeResponse user, String accessToken, String refreshToken) {
    }

    private static final String INVALID_OTP_MESSAGE = "That code is invalid or has expired.";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Those details do not match an account.";

    /**
     * A real Argon2id hash of a password nobody has. Verifying it for an unknown identifier makes a
     * failed login take the same time whether or not the account exists.
     */
    private static final String DUMMY_HASH =
            "$argon2id$v=19$m=16384,t=2,p=1$EMybXn+G3QFwCbEXkXAe8w$ALGN65xt3T83GDTgRQs3muTG5H6v/KyCnA7fWzzuP94";

    private final UserService userService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLog auditLog;
    private final RateLimitGuard rateLimitGuard;

    AuthService(UserService userService, OtpService otpService,
            RefreshTokenService refreshTokenService, JwtService jwtService,
            PasswordEncoder passwordEncoder, AuditLog auditLog, RateLimitGuard rateLimitGuard) {
        this.userService = userService;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
        this.rateLimitGuard = rateLimitGuard;
    }

    /**
     * Always ends in the same 202. A new phone gets an account and a code; a phone that is already
     * registered but unverified gets another code; a verified phone and a taken email get nothing.
     * This method is deliberately not transactional: a duplicate must not roll back the audit entry.
     */
    void register(RegisterRequest request, String clientIp) {
        String phone = PhoneNumbers.normalize(request.phone(), request.country());
        checkOtpSendLimits(phone, clientIp);

        Optional<UserCredentials> existing = userService.findCredentialsByPhone(phone);
        if (existing.isPresent()) {
            UserCredentials credentials = existing.get();
            if (!credentials.phoneVerified()) {
                otpService.issue(phone);
            }
            auditLog.record(credentials.userId(), "auth.register.duplicate_phone", Map.of());
            return;
        }

        try {
            UserView created = userService.create(new NewUser(
                    phone,
                    passwordEncoder.encode(request.password()),
                    request.firstName().trim(),
                    request.lastName().trim(),
                    request.country(),
                    defaultLanguage(request.country()),
                    request.email(),
                    request.nationalId()));
            auditLog.record(created.id(), "auth.register", Map.of("country", request.country()));
            otpService.issue(phone);
        } catch (DataIntegrityViolationException e) {
            // The phone or the email is taken (a race, or a taken email): answer as if it worked.
            auditLog.record(null, "auth.register.conflict", Map.of());
        }
    }

    /** Checks the code, marks the phone verified and starts a session. */
    Session verifyOtp(VerifyOtpRequest request) {
        String phone = PhoneNumbers.normalizeAny(request.phone());
        UserCredentials credentials = userService.findCredentialsByPhone(phone)
                .orElseThrow(() -> ApiException.badRequest("invalid_otp", INVALID_OTP_MESSAGE));

        if (!otpService.consume(phone, request.code())) {
            auditLog.record(credentials.userId(), "auth.verify_otp.failed", Map.of());
            throw ApiException.badRequest("invalid_otp", INVALID_OTP_MESSAGE);
        }

        userService.markPhoneVerified(credentials.userId());
        auditLog.record(credentials.userId(), "auth.verify_otp", Map.of());
        return startSession(credentials.userId());
    }

    /** Always ends in the same 202; a code is only sent when there is an unverified account. */
    void resendOtp(ResendOtpRequest request, String clientIp) {
        String phone = PhoneNumbers.normalizeAny(request.phone());
        checkOtpSendLimits(phone, clientIp);

        userService.findCredentialsByPhone(phone)
                .filter(credentials -> !credentials.phoneVerified())
                .ifPresent(credentials -> {
                    otpService.issue(phone);
                    auditLog.record(credentials.userId(), "auth.otp.resend", Map.of());
                });
    }

    /**
     * Two independent caps on sending an SMS: per phone number (3 per 10 minutes, so nobody can
     * flood one handset) and per client IP (10 per hour, so one host cannot walk through many
     * numbers). Both count every attempt, whether or not the account exists.
     */
    private void checkOtpSendLimits(String phone, String clientIp) {
        rateLimitGuard.check("otp:" + phone, RateLimitPolicies.OTP_SEND);
        rateLimitGuard.check("otp:ip:" + clientIp, RateLimitPolicies.OTP_SEND_PER_IP);
    }

    Session startSession(UUID userId) {
        UserView user = userService.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("unauthenticated", "Sign in again."));
        return new Session(MeResponse.from(user), jwtService.issueAccessToken(userId),
                refreshTokenService.startFamily(userId));
    }

    /** Phone or email plus password. Every failure returns the same generic error. */
    Session login(LoginRequest request, String clientIp) {
        rateLimitGuard.check("login:ip:" + clientIp, RateLimitPolicies.LOGIN_PER_IP);
        String identifier = request.identifier().trim();

        Optional<UserCredentials> found = identifier.contains("@")
                ? userService.findCredentialsByEmail(identifier.toLowerCase(Locale.ROOT))
                : findByPhoneQuietly(identifier);

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            auditLog.record(null, "auth.login.failed", Map.of("reason", "unknown_identifier"));
            throw ApiException.unauthorized("invalid_credentials", INVALID_CREDENTIALS_MESSAGE);
        }

        UserCredentials credentials = found.get();
        rateLimitGuard.check("login:account:" + credentials.userId(),
                RateLimitPolicies.LOGIN_PER_ACCOUNT);

        if (!passwordEncoder.matches(request.password(), credentials.passwordHash())) {
            auditLog.record(credentials.userId(), "auth.login.failed",
                    Map.of("reason", "bad_password"));
            throw ApiException.unauthorized("invalid_credentials", INVALID_CREDENTIALS_MESSAGE);
        }
        if (!credentials.phoneVerified()) {
            auditLog.record(credentials.userId(), "auth.login.failed",
                    Map.of("reason", "phone_not_verified"));
            throw ApiException.forbidden("phone_not_verified",
                    "Verify your phone number to finish signing in.");
        }

        auditLog.record(credentials.userId(), "auth.login", Map.of());
        return startSession(credentials.userId());
    }

    /** Rotates the refresh token and mints a new access token. */
    Session refresh(String presentedToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(presentedToken);
        UserView user = userService.findById(rotation.userId())
                .orElseThrow(() -> ApiException.unauthorized("invalid_refresh_token",
                        "Your session has expired. Sign in again."));
        auditLog.record(rotation.userId(), "auth.refresh", Map.of());
        return new Session(MeResponse.from(user), jwtService.issueAccessToken(rotation.userId()),
                rotation.token());
    }

    /** Revokes the whole family behind the presented token. Unknown or missing tokens are fine. */
    void logout(String presentedToken) {
        refreshTokenService.revokeFamilyOf(presentedToken)
                .ifPresent(userId -> auditLog.record(userId, "auth.logout", Map.of()));
    }

    /** An identifier that is not a supported phone number is simply an unknown account. */
    private Optional<UserCredentials> findByPhoneQuietly(String identifier) {
        try {
            return userService.findCredentialsByPhone(PhoneNumbers.normalizeAny(identifier));
        } catch (ApiException e) {
            return Optional.empty();
        }
    }

    private static String defaultLanguage(String country) {
        return "CD".equals(country) ? "fr" : "en";
    }
}
