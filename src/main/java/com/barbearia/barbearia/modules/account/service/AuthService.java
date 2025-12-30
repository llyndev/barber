package com.barbearia.barbearia.modules.account.service;

import com.barbearia.barbearia.modules.account.dto.request.AuthRequest;
import com.barbearia.barbearia.modules.account.dto.response.AuthResponse;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.account.mapper.UserMapper;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
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
