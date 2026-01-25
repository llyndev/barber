package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.modules.account.dto.request.AuthRequest;
import com.barbearia.barbearia.modules.account.dto.request.ForgotPasswordRequest;
import com.barbearia.barbearia.modules.account.dto.request.PasswordResetTokenRequest;
import com.barbearia.barbearia.modules.account.dto.response.AuthResponse;
import com.barbearia.barbearia.modules.account.dto.response.PasswordResetTokenResponse;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
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
    private final PasswordResetTokenService passwordResetTokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
        return ResponseEntity.ok().body("Se o e-mail existir, enviaremos um link de recuperação.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetTokenRequest request) {

        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body("Credenciais invalidas");
        }

        passwordResetTokenService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().body("Senha atualizada com sucesso");
    }


}
