package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.UserRequest;
import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.mapper.UserMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public AppUser getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public AppUser register(UserRequest userRequest) {
        if (!userRequest.getPassword().equals(userRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Invalid credential");
        }

        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        AppUser user = AppUser.builder()
                .name(userRequest.getName())
                .email(userRequest.getEmail())
                .telephone(userRequest.getTelephone())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .role(AppUser.Role.CLIENT)
                .build();

        return userRepository.save(user);
    }
}
