package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.modules.account.dto.request.CompleteRegistrationRequest;
import com.barbearia.barbearia.modules.account.dto.request.RegisterRequest;
import com.barbearia.barbearia.modules.account.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        registerService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> completeRegistration(@RequestBody CompleteRegistrationRequest request) {
        registerService.completeRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
