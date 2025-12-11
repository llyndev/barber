package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.AuthRequest;
import com.barbearia.barbearia.dto.response.AuthResponse;
import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.mapper.UserMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.repository.UserRepository;
import com.barbearia.barbearia.security.JwtUtil;
import com.barbearia.barbearia.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("User not authenticated");
        }

        AppUser user = userRepository.findById(userDetails.user().getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toDTO(user);
    }
}
