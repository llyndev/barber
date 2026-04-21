package com.barbearia.barbearia.modules.googlecalender.service;

import com.barbearia.barbearia.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleCalendarOAuthStateService {

    private static final long STATE_TTL_MINUTES = 10;

    private final Map<String, OAuthStateEntry> states = new ConcurrentHashMap<>();

    public OAuthStateEntry create(Long userId, Long businessId, String redirectUri) {
        cleanupExpired();
        String state = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(STATE_TTL_MINUTES);

        OAuthStateEntry entry = new OAuthStateEntry(state, userId, businessId, redirectUri, expiresAt);
        states.put(state, entry);
        return entry;
    }

    public void validateAndConsume(String state, Long userId, Long businessId, String redirectUri) {
        OAuthStateEntry entry = states.remove(state);
        if (entry == null) {
            throw new InvalidRequestException("State OAuth invalido ou ja utilizado.");
        }

        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("State OAuth expirado. Gere uma nova URL.");
        }

        if (!entry.userId().equals(userId) || !entry.businessId().equals(businessId)) {
            throw new InvalidRequestException("State OAuth nao pertence ao usuario/barbearia atual.");
        }

        if (!entry.redirectUri().equals(redirectUri)) {
            throw new InvalidRequestException("Redirect URI diferente da URL autorizada.");
        }
    }

    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        states.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    public record OAuthStateEntry(
            String value,
            Long userId,
            Long businessId,
            String redirectUri,
            LocalDateTime expiresAt
    ) {
    }
}

