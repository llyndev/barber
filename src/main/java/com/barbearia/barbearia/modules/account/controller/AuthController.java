package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.modules.account.dto.request.AuthRequest;
import com.barbearia.barbearia.modules.account.dto.request.ForgotPasswordRequest;
import com.barbearia.barbearia.modules.account.dto.request.GoogleAuthRequest;
import com.barbearia.barbearia.modules.account.dto.request.PasswordResetTokenRequest;
import com.barbearia.barbearia.modules.account.dto.response.AuthResponse;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.account.service.GoogleAuthService;
import com.barbearia.barbearia.modules.account.service.PasswordResetTokenService;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.account.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetTokenService passwordResetTokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
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


}
