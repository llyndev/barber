package com.barbearia.barbearia.modules.googlecalender.service;

import com.barbearia.barbearia.exception.ExternalServiceException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.modules.googlecalender.dto.request.GoogleCalendarConnectRequest;
import com.barbearia.barbearia.modules.googlecalender.dto.response.GoogleCalendarAuthorizationUrlResponse;
import com.barbearia.barbearia.modules.googlecalender.dto.response.GoogleCalendarConnectionResponse;
import com.barbearia.barbearia.modules.googlecalender.model.GoogleCalendarConnection;
import com.barbearia.barbearia.modules.googlecalender.repository.GoogleCalendarConnectionRepository;
import com.barbearia.barbearia.modules.scheduling.model.AppointmentStatus;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.scheduling.repository.SchedulingRepository;
import com.barbearia.barbearia.tenant.BusinessContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalenderService {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_EVENTS_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars";
    private static final String GOOGLE_AUTH_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String DEFAULT_CALENDAR_ID = "primary";

    private final WebClient.Builder webClientBuilder;
    private final BusinessRepository businessRepository;
    private final UserBusinessRepository userBusinessRepository;
    private final GoogleCalendarConnectionRepository connectionRepository;
    private final SchedulingRepository schedulingRepository;
    private final GoogleCalendarOAuthStateService oauthStateService;

    @Value("${spring.application.name:barbercuttz-api}")
    private String appName;

    @Value("${google.auth.client-id:}")
    private String googleClientId;

    @Value("${google.auth.client-secret:}")
    private String googleClientSecret;

    @Value("${google.auth.scopes:https://www.googleapis.com/auth/calendar}")
    private String googleScopes;

    @Transactional(readOnly = true)
    public GoogleCalendarAuthorizationUrlResponse createAuthorizationUrl(Long userId, String redirectUri) {
        validateGoogleOAuthConfig();

        if (redirectUri == null || redirectUri.isBlank()) {
            throw new InvalidRequestException("redirectUri obrigatorio para iniciar OAuth.");
        }

        Long businessId = getBusinessIdFromContext();
        validateMembership(userId, businessId);

        GoogleCalendarOAuthStateService.OAuthStateEntry stateEntry = oauthStateService.create(userId, businessId, redirectUri);

        String encodedRedirectUri = urlEncode(redirectUri);
        String encodedClientId = urlEncode(googleClientId);
        String encodedScope = urlEncode(googleScopes);
        String encodedState = urlEncode(stateEntry.value());

        StringJoiner query = new StringJoiner("&");
        query.add("client_id=" + encodedClientId);
        query.add("redirect_uri=" + encodedRedirectUri);
        query.add("response_type=code");
        query.add("scope=" + encodedScope);
        query.add("access_type=offline");
        query.add("include_granted_scopes=true");
        query.add("prompt=consent");
        query.add("state=" + encodedState);

        return new GoogleCalendarAuthorizationUrlResponse(
                GOOGLE_AUTH_BASE_URL + "?" + query,
                stateEntry.value(),
                stateEntry.expiresAt()
        );
    }

    @Transactional
    public GoogleCalendarConnectionResponse connectCurrentUser(Long userId, GoogleCalendarConnectRequest request) {
        validateGoogleOAuthConfig();

        Long businessId = getBusinessIdFromContext();
        validateMembership(userId, businessId);
        oauthStateService.validateAndConsume(request.state(), userId, businessId, request.redirectUri());

        JsonNode tokenNode = exchangeAuthorizationCode(request.code(), request.redirectUri());

        String accessToken = text(tokenNode, "access_token");
        String refreshToken = text(tokenNode, "refresh_token");
        Long expiresIn = longValue(tokenNode, "expires_in", 3600L);

        if (accessToken == null || accessToken.isBlank()) {
            throw new ExternalServiceException("Google nao retornou access_token.");
        }

        String googleEmail = fetchGoogleEmail(accessToken);
        String calendarId = (request.calendarId() == null || request.calendarId().isBlank())
                ? DEFAULT_CALENDAR_ID
                : request.calendarId();

        GoogleCalendarConnection connection = connectionRepository
                .findByUserIdAndBusinessId(userId, businessId)
                .orElseGet(GoogleCalendarConnection::new);

        connection.setUser(userBusinessRepository.findByUserIdAndBusinessId(userId, businessId)
                .orElseThrow(() -> new SecurityException("Usuario nao pertence a barbearia"))
                .getUser());
        connection.setBusiness(getBusinessEntityFromContext());
        connection.setGoogleEmail(googleEmail);
        connection.setCalendarId(calendarId);
        connection.setAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            connection.setRefreshToken(refreshToken);
        }
        connection.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        connection.setActive(true);
        connection.setUpdatedAt(LocalDateTime.now());

        GoogleCalendarConnection saved = connectionRepository.save(connection);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GoogleCalendarConnectionResponse getStatus(Long userId) {
        Long businessId = getBusinessIdFromContext();
        Optional<GoogleCalendarConnection> opt = connectionRepository
                .findByUserIdAndBusinessIdAndActiveTrue(userId, businessId);

        return opt.map(this::toResponse)
                .orElse(new GoogleCalendarConnectionResponse(false, null, null, null));
    }

    @Transactional
    public void disconnectCurrentUser(Long userId) {
        Long businessId = getBusinessIdFromContext();
        connectionRepository.findByUserIdAndBusinessId(userId, businessId)
                .ifPresent(connection -> {
                    connection.setActive(false);
                    connection.setUpdatedAt(LocalDateTime.now());
                    connectionRepository.save(connection);
                });
    }

    @Async
    @Transactional
    public void syncSchedulingCreated(Scheduling scheduling) {
        if (scheduling == null || scheduling.getBarber() == null || scheduling.getBusiness() == null) {
            return;
        }

        Optional<GoogleCalendarConnection> connectionOpt = connectionRepository
                .findByUserIdAndBusinessIdAndActiveTrue(scheduling.getBarber().getId(), scheduling.getBusiness().getId());

        if (connectionOpt.isEmpty()) {
            return;
        }

        GoogleCalendarConnection connection = ensureValidAccessToken(connectionOpt.get());

        JsonNode response = createEvent(connection, scheduling);
        String eventId = text(response, "id");
        if (eventId != null && !eventId.isBlank()) {
            scheduling.setGoogleEventId(eventId);
            schedulingRepository.save(scheduling);
        }
    }

    @Transactional
    public void syncSchedulingUpdated(Scheduling scheduling) {
        if (scheduling == null || scheduling.getBarber() == null || scheduling.getBusiness() == null) {
            return;
        }

        Optional<GoogleCalendarConnection> connectionOpt = connectionRepository
                .findByUserIdAndBusinessIdAndActiveTrue(scheduling.getBarber().getId(), scheduling.getBusiness().getId());

        if (connectionOpt.isEmpty()) {
            return;
        }

        GoogleCalendarConnection connection = ensureValidAccessToken(connectionOpt.get());

        if (scheduling.getGoogleEventId() == null || scheduling.getGoogleEventId().isBlank()) {
            syncSchedulingCreated(scheduling);
            return;
        }

        updateEvent(connection, scheduling);
    }

    @Transactional
    public void syncSchedulingCanceled(Scheduling scheduling) {
        if (scheduling == null || scheduling.getBarber() == null || scheduling.getBusiness() == null) {
            return;
        }
        if (scheduling.getGoogleEventId() == null || scheduling.getGoogleEventId().isBlank()) {
            return;
        }

        Optional<GoogleCalendarConnection> connectionOpt = connectionRepository
                .findByUserIdAndBusinessIdAndActiveTrue(scheduling.getBarber().getId(), scheduling.getBusiness().getId());

        if (connectionOpt.isEmpty()) {
            return;
        }

        GoogleCalendarConnection connection = ensureValidAccessToken(connectionOpt.get());
        deleteEvent(connection, scheduling.getGoogleEventId());
        scheduling.setGoogleEventId(null);
        schedulingRepository.save(scheduling);
    }

    private JsonNode exchangeAuthorizationCode(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            return webClientBuilder.build()
                    .post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao trocar authorization code do Google.");
        }
    }

    private GoogleCalendarConnection ensureValidAccessToken(GoogleCalendarConnection connection) {
        if (connection.getTokenExpiresAt() == null || connection.getTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            return connection;
        }

        if (connection.getRefreshToken() == null || connection.getRefreshToken().isBlank()) {
            throw new ExternalServiceException("Google refresh token ausente. Reconecte sua conta.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("refresh_token", connection.getRefreshToken());
        body.add("grant_type", "refresh_token");

        try {
            JsonNode refreshed = webClientBuilder.build()
                    .post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String accessToken = text(refreshed, "access_token");
            Long expiresIn = longValue(refreshed, "expires_in", 3600L);

            if (accessToken == null || accessToken.isBlank()) {
                throw new ExternalServiceException("Google nao retornou novo access_token.");
            }

            connection.setAccessToken(accessToken);
            connection.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            connection.setUpdatedAt(LocalDateTime.now());
            return connectionRepository.save(connection);
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao renovar token do Google Calendar.");
        }
    }

    private JsonNode createEvent(GoogleCalendarConnection connection, Scheduling scheduling) {
        String calendarId = UriUtils.encodePathSegment(connection.getCalendarId(), StandardCharsets.UTF_8);

        try {
            return webClientBuilder.build()
                    .post()
                    .uri(GOOGLE_EVENTS_BASE_URL + "/" + calendarId + "/events")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + connection.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(buildEventPayload(scheduling))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao criar evento no Google Calendar.");
        }
    }

    private void updateEvent(GoogleCalendarConnection connection, Scheduling scheduling) {
        String calendarId = UriUtils.encodePathSegment(connection.getCalendarId(), StandardCharsets.UTF_8);
        String eventId = UriUtils.encodePathSegment(scheduling.getGoogleEventId(), StandardCharsets.UTF_8);

        try {
            webClientBuilder.build()
                    .put()
                    .uri(GOOGLE_EVENTS_BASE_URL + "/" + calendarId + "/events/" + eventId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + connection.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(buildEventPayload(scheduling))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao atualizar evento no Google Calendar.");
        }
    }

    private void deleteEvent(GoogleCalendarConnection connection, String googleEventId) {
        String calendarId = UriUtils.encodePathSegment(connection.getCalendarId(), StandardCharsets.UTF_8);
        String eventId = UriUtils.encodePathSegment(googleEventId, StandardCharsets.UTF_8);

        try {
            webClientBuilder.build()
                    .delete()
                    .uri(GOOGLE_EVENTS_BASE_URL + "/" + calendarId + "/events/" + eventId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + connection.getAccessToken())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            throw new ExternalServiceException("Falha ao remover evento no Google Calendar.");
        }
    }

    private Map<String, Object> buildEventPayload(Scheduling scheduling) {
        String timezone = resolveTimezone(scheduling);
        LocalDateTime startDateTime = scheduling.getDateTime();
        LocalDateTime endDateTime = scheduling.getDateTime().plusMinutes(calculateDurationInMinutes(scheduling));

        String clientName = scheduling.getUser() != null ? scheduling.getUser().getName() : scheduling.getClientName();
        if (clientName == null || clientName.isBlank()) {
            clientName = "Cliente";
        }

        List<String> serviceNames = scheduling.getBarberService().stream()
                .map(service -> service.getNameService())
                .toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", "Agendamento Barbercuttz - " + clientName);
        payload.put("description", "Servicos: " + String.join(", ", serviceNames)
                + "\\nBarbeiro: " + scheduling.getBarber().getName()
                + "\\nStatus: " + scheduling.getStates().name()
                + "\\nOrigem: " + appName);

        payload.put("start", Map.of(
                "dateTime", formatAsRfc3339(startDateTime, timezone),
                "timeZone", timezone
        ));

        payload.put("end", Map.of(
                "dateTime", formatAsRfc3339(endDateTime, timezone),
                "timeZone", timezone
        ));

        if (scheduling.getUser() != null && scheduling.getUser().getEmail() != null && !scheduling.getUser().getEmail().isBlank()) {
            payload.put("attendees", List.of(Map.of("email", scheduling.getUser().getEmail())));
        }

        if (scheduling.getStates() == AppointmentStatus.CANCELED) {
            payload.put("status", "cancelled");
        }

        return payload;
    }

    private String fetchGoogleEmail(String accessToken) {
        try {
            JsonNode userInfo = webClientBuilder.build()
                    .get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return text(userInfo, "email");
        } catch (Exception ex) {
            log.warn("Nao foi possivel ler email Google da conexao: {}", ex.getMessage());
            return null;
        }
    }

    private String formatAsRfc3339(LocalDateTime dateTime, String timezone) {
        return dateTime.atZone(ZoneId.of(timezone))
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private int calculateDurationInMinutes(Scheduling scheduling) {
        return scheduling.getBarberService().stream()
                .mapToInt(service -> (service.getDurationInMinutes() != null && service.getDurationInMinutes() > 0)
                        ? service.getDurationInMinutes()
                        : 15)
                .sum();
    }

    private String resolveTimezone(Scheduling scheduling) {
        if (scheduling.getBusiness() != null && scheduling.getBusiness().getTimezone() != null
                && !scheduling.getBusiness().getTimezone().isBlank()) {
            return scheduling.getBusiness().getTimezone();
        }
        return "America/Sao_Paulo";
    }

    private GoogleCalendarConnectionResponse toResponse(GoogleCalendarConnection connection) {
        return new GoogleCalendarConnectionResponse(
                connection.isActive(),
                connection.getGoogleEmail(),
                connection.getCalendarId(),
                connection.getTokenExpiresAt()
        );
    }

    private Long getBusinessIdFromContext() {
        String businessId = BusinessContext.getBusinessId();
        if (businessId == null || businessId.isBlank()) {
            throw new InvalidRequestException("X-Business-Slug obrigatorio para Google Calendar.");
        }

        try {
            return Long.parseLong(businessId);
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Business ID invalido no contexto.");
        }
    }

    private Business getBusinessEntityFromContext() {
        return businessRepository.findById(getBusinessIdFromContext())
                .orElseThrow(() -> new InvalidRequestException("Business nao encontrado para conectar Google Calendar."));
    }

    private void validateMembership(Long userId, Long businessId) {
        boolean belongs = userBusinessRepository.findByUserIdAndBusinessId(userId, businessId).isPresent();
        if (!belongs) {
            throw new SecurityException("Usuario nao pertence a barbearia selecionada.");
        }
    }

    private void validateGoogleOAuthConfig() {
        if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new IllegalStateException("Google OAuth nao configurado. Defina google.auth.client-id e google.auth.client-secret.");
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private Long longValue(JsonNode node, String field, long defaultValue) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return defaultValue;
        }
        return node.get(field).asLong(defaultValue);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}


