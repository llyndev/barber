package com.barbearia.barbearia.modules.account.service;

import com.barbearia.barbearia.exception.BadRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.account.dto.response.PasswordResetTokenResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.model.PasswordResetToken;
import com.barbearia.barbearia.modules.account.repository.PasswordResetTokenRepository;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.email.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.reset.base-url:http://localhost:8080}")
    private String baseUrl;


    @Transactional
    public void requestReset(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new ResourceNotFoundException("The email address does not exist.");
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            var prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setToken(generateToken());
            prt.setExpirationDate(Instant.now().plus(Duration.ofMinutes(30)));
            prt.setUsed(false);

            tokenRepository.invalidateAllActiveByUser(user.getId());
            tokenRepository.save(prt);

            String link = baseUrl + "/reset-password?token=" + prt.getToken();
            emailService.sendReset(user.getEmail(), link);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        var prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (prt.isUsed()) throw new BadRequestException("Token already used");
        if (prt.getExpirationDate().isBefore(Instant.now())) throw new BadRequestException("Expired token");

        var user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
