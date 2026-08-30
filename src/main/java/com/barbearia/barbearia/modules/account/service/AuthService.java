package com.barbearia.barbearia.modules.account.service;

import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.account.mapper.UserMapper;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    public Authentication login(String rawEmail, String password) {
        String email = normalizeEmail(rawEmail);
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("User not authenticated");
        }

        AppUser user = userRepository.findById(userDetails.id())
            .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toDTO(user);
    }
}
