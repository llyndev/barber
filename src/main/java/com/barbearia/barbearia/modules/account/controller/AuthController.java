package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.modules.account.dto.request.AuthRequest;
import com.barbearia.barbearia.modules.account.dto.response.AuthResponse;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.account.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(authService.getMe(userDetails));
    }
}
