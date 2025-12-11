package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.PromoteToOwnerRequest;
import com.barbearia.barbearia.dto.request.UpdateRoleRequest;
import com.barbearia.barbearia.dto.request.UserRequest;
import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.UserMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.model.BusinessRole;
import com.barbearia.barbearia.model.UserBusiness;
import com.barbearia.barbearia.repository.UserBusinessRepository;
import com.barbearia.barbearia.repository.UserRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.tenant.BusinessContext;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserBusinessRepository userBusinessRepository;

    public List<UserResponse> findAll() {
        List<AppUser> users = userRepository.findAll();
        return users.stream()
                .map(userMapper::toDTO)
                .toList();
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

        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null || businessIdStr.isBlank()) {
            return List.of();
        }

        Long businessId = Long.parseLong(businessIdStr);

        List<UserBusiness> memberships = userBusinessRepository
                .findAllByBusinessIdAndRole(businessId, BusinessRole.BARBER);

        return memberships.stream()
                .map(UserBusiness::getUser)
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

        user.setPlatformRole(AppUser.PlatformRole.valueOf(role.role()));

        AppUser updateUserRole = userRepository.save(user);

        return userMapper.toDTO(updateUserRole);
    }

    @Transactional
    public UserResponse promoteToOwner(Long id, PromoteToOwnerRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Atualiza role para BUSINESS_OWNER
        user.setPlatformRole(AppUser.PlatformRole.BUSINESS_OWNER);
        user.setPlantType(request.plantType().name());
        user.setBusinessCreator(true);
        
        // Se houver data de expiração, atualiza
        if (request.planExpirationDate() != null) {
            user.setDateExpirationAccount(request.planExpirationDate().toLocalDate());
        }

        AppUser updatedUser = userRepository.save(user);

        return userMapper.toDTO(updatedUser);
    }
}
