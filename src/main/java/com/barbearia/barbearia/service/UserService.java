package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.UpdateRoleRequest;
import com.barbearia.barbearia.dto.request.UserRequest;
import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.UserMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.repository.UserRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;
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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDTO)
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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

    public UserResponse getLoggedUser(UserDetailsImpl userDetails) {
        AppUser loggedUser = userDetails.user();
        return userMapper.toDTO(loggedUser);
    }

    public List<UserResponse> listBarbers() {
        List<AppUser> barberUser = userRepository.findAllByRole(AppUser.Role.BARBER);
            return barberUser.stream()
                    .map(userMapper::toDTO)
                    .toList();
    }

    public UserResponse update(Long id, UserRequest userRequest) {
        AppUser appUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRequest.name() != null) {
            appUser.setName(userRequest.name());
        }

        if (userRequest.email() != null) {
            appUser.setEmail(userRequest.email());
        }

        if (userRequest.telephone() != null) {
            appUser.setTelephone(userRequest.telephone());
        }

        if (userRequest.password() != null) {
            appUser.setPassword(passwordEncoder.encode(userRequest.password()));
        }

        AppUser userUpdate = userRepository.save(appUser);

        return userMapper.toDTO(userUpdate);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public UserResponse updateRole(Long id, UpdateRoleRequest role) {
        if (role == null ) {
            throw new IllegalArgumentException("Role null");
        }

        AppUser user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRole(AppUser.Role.valueOf(role.role()));

        AppUser updateUserRole = userRepository.save(user);

        return userMapper.toDTO(updateUserRole);
    }
}
