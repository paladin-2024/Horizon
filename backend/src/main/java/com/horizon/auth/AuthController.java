package com.horizon.auth;

import com.horizon.common.error.ApiException;
import com.horizon.common.idempotency.Idempotent;
import com.horizon.common.security.CurrentUser;
import com.horizon.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService authService;
    private final TokenCookies tokenCookies;
    private final ClientIp clientIp;
    private final UserService userService;

    AuthController(AuthService authService, TokenCookies tokenCookies, ClientIp clientIp,
            UserService userService) {
        this.authService = authService;
        this.tokenCookies = tokenCookies;
        this.clientIp = clientIp;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Idempotent
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        authService.register(request, clientIp.of(httpRequest));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-otp")
    ResponseEntity<MeResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthService.Session session = authService.verifyOtp(request);
        tokenCookies.write(httpRequest, httpResponse, session.accessToken(), session.refreshToken());
        return ResponseEntity.ok(session.user());
    }

    @PostMapping("/resend-otp")
    ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {
        authService.resendOtp(request, clientIp.of(httpRequest));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    ResponseEntity<MeResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthService.Session session = authService.login(request, clientIp.of(httpRequest));
        tokenCookies.write(httpRequest, httpResponse, session.accessToken(), session.refreshToken());
        return ResponseEntity.ok(session.user());
    }

    @PostMapping("/refresh")
    ResponseEntity<Void> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String presented = TokenCookies.read(httpRequest, TokenCookies.REFRESH_COOKIE).orElse(null);
        AuthService.Session session = authService.refresh(presented);
        tokenCookies.write(httpRequest, httpResponse, session.accessToken(), session.refreshToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(TokenCookies.read(httpRequest, TokenCookies.REFRESH_COOKIE).orElse(null));
        tokenCookies.clear(httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/me")
    MeResponse me() {
        return userService.findById(CurrentUser.id())
                .map(MeResponse::from)
                .orElseThrow(() -> ApiException.unauthorized("unauthenticated",
                        "Sign in to continue."));
    }
}
