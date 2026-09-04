package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.modules.account.dto.request.*;
import com.barbearia.barbearia.modules.account.dto.response.AuthResponse;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.account.service.GoogleAuthService;
import com.barbearia.barbearia.modules.account.service.PasswordResetTokenService;
import com.barbearia.barbearia.modules.account.service.RefreshTokenService;
import com.barbearia.barbearia.security.RefreshCookieFactory;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.account.service.AuthService;
import com.barbearia.barbearia.security.ratelimit.ClientIpResolver;
import com.barbearia.barbearia.security.ratelimit.RateLimiterService;
import com.barbearia.barbearia.security.ratelimit.RateLimiterService.LimitType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final RateLimiterService rateLimiter;
    private final RefreshTokenService refreshTokenService;
    private final RefreshCookieFactory cookieFactory;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request,
                                               HttpServletRequest httpRequest) {

        String clientIp = ClientIpResolver.resolve(httpRequest);

        rateLimiter.checkAvaiable(clientIp, LimitType.LOGIN);
        rateLimiter.checkAvaiable(request.email(), LimitType.LOGIN);

        Authentication auth;
        try {
            auth = authService.login(request.email(), request.password());
        } catch (AuthenticationException ex) {
            rateLimiter.consume(clientIp, LimitType.LOGIN);
            rateLimiter.consume(request.email(), LimitType.LOGIN);
            throw ex;
        }

        var user = (UserDetailsImpl) auth.getPrincipal();
        TokenPair tokens = refreshTokenService.issue(user, httpRequest);

        return respondWithTokens(tokens, user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "${app.auth.cookie.name:refresh_token}", required = false)
            String refreshToken,
            HttpServletRequest request) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRequestException("Refresh token away");
        }

        TokenPair tokens = refreshTokenService.rotate(refreshToken, request);

        return respondWithTokens(tokens, tokens.user());
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.auth.cookie.name:refresh_token}", required = false)
            String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody @Valid GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticate(request.idToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(authService.getMe(userDetails));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        passwordResetTokenService.requestReset(request.email());
        return ResponseEntity.ok().body("If the email address exists, we will send a recovery link.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetTokenRequest request) {

        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body("Invalid credentials");
        }

        passwordResetTokenService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().body("Password updated successfully.");
    }

    private ResponseEntity<AuthResponse> respondWithTokens(TokenPair tokens,
                                                           UserDetailsImpl user) {
        ResponseCookie cookie = cookieFactory.create(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(
                        tokens.accessToken(),
                        tokens.expiresIn(),
                        user.id(),
                        user.email(),
                        user.platformRole()
                ));
    }

}
