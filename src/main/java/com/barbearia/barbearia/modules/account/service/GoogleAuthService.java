package com.barbearia.barbearia.modules.account.service;

import com.barbearia.barbearia.exception.ExternalServiceException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.modules.account.dto.response.AuthResponse;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.security.JwtUtil;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String DEFAULT_PHONE = "(00)00000-0000";

    private final WebClient.Builder webClientBuilder;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${google.auth.client-id:}")
    private String googleClientId;

    @Transactional
    public AuthResponse authenticate(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Google auth nao configurado. Defina google.auth.client-id.");
        }

        JsonNode payload = fetchTokenInfo(idToken);
        String audience = text(payload, "aud");
        if (!googleClientId.equals(audience)) {
            throw new InvalidRequestException("Token do Google invalido para este app.");
        }

        if (!Boolean.parseBoolean(text(payload, "email_verified"))) {
            throw new InvalidRequestException("Conta Google precisa ter email verificado.");
        }

        String subject = text(payload, "sub");
        String email = text(payload, "email");
        String name = text(payload, "name");
        String picture = text(payload, "picture");

        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new InvalidRequestException("Token do Google sem dados obrigatorios.");
        }

        AppUser user = userRepository.findByGoogleSubject(subject)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> createGoogleUser(email, name));

        if (user.getGoogleSubject() == null || user.getGoogleSubject().isBlank()) {
            user.setGoogleSubject(subject);
        }
        if (user.getGooglePicture() == null || user.getGooglePicture().isBlank()) {
            user.setGooglePicture(picture);
        }
        if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
            user.setName(name);
        }

        userRepository.save(user);

        String token = jwtUtil.generateToken(new UserDetailsImpl(user));
        return new AuthResponse(token);
    }

    private AppUser createGoogleUser(String email, String name) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName((name == null || name.isBlank()) ? "Usuario Google" : name);
        user.setTelephone(DEFAULT_PHONE);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPlatformRole(AppUser.PlatformRole.CLIENT);
        user.setActive(true);
        return user;
    }

    private JsonNode fetchTokenInfo(String idToken) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(GOOGLE_TOKEN_INFO_URL + "?id_token={idToken}", idToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao validar token com Google.");
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}


