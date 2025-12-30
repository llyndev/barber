package com.barbearia.barbearia.modules.account.service;

import com.barbearia.barbearia.modules.account.dto.request.CompleteRegistrationRequest;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.leads.model.Lead;
import com.barbearia.barbearia.modules.leads.model.LeadStatus;
import com.barbearia.barbearia.modules.account.dto.request.RegisterRequest;
import com.barbearia.barbearia.modules.leads.repository.LeadRepository;
import com.barbearia.barbearia.modules.account.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        newUser.setPlatformRole(AppUser.PlatformRole.CLIENT);
        newUser.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(newUser);
    }

    @Transactional
    public void completeRegistration(CompleteRegistrationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password do not match");
        }

        Lead lead = leadRepository.findByRegistrationToken(request.token())
            .orElseThrow(() -> new InvalidRequestException("Invalid or expired token"));

        if (lead.getTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Token expired");
        }

        if (userRepository.findByEmail(lead.getEmail()).isPresent()) {
            throw new IllegalStateException("User alredy exists");
        }

        AppUser newUser = new AppUser();
        newUser.setName(lead.getName());
        newUser.setEmail(lead.getEmail());
        newUser.setTelephone(lead.getTelephone());
        newUser.setDocument(request.document());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setPlatformRole(AppUser.PlatformRole.BUSINESS_OWNER);
        newUser.setPlantType(lead.getPlan());
        newUser.setActive(true);
        newUser.setDateExpirationAccount(lead.getAccountExpirationDate());

        userRepository.save(newUser);

        lead.setRegistrationToken(null);
        lead.setStatus(LeadStatus.REGISTERED);
        leadRepository.save(lead);
    }
}
