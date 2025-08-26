package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.UserRequest;
import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.mapper.UserMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.security.JwtUtil;
import com.barbearia.barbearia.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<UserResponse> getAll() {
        List<AppUser> users = userService.findAll();

        return users.stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest userRequest) {
        try {
            AppUser appUser = userService.register(userRequest);
            return ResponseEntity.ok("Sucess" + appUser.getEmail());
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body("Error: " + exception.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequest userRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userRequest.getEmail(), userRequest.getPassword())
            );

            String token = jwtUtil.generateToken(userRequest.getEmail());
            return ResponseEntity.ok(token);
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(401).body("Invalid credentials.");
        }
    }

}
