package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.RegisterRequest;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements RegisterService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email already exists");
        }

        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Some invalid credential");
        }

        AppUser newUser = new AppUser();
        newUser.setName(request.name());
        newUser.setEmail(request.email());
        newUser.setTelephone(request.telephone());
        newUser.setRole(AppUser.Role.CLIENT);
        newUser.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(newUser);
    }
}
