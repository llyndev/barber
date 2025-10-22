package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.AuthRequest;
import com.barbearia.barbearia.dto.response.AuthResponse;
import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.service.AuthService;
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
