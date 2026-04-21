package com.barbearia.barbearia.modules.googlecalender.controller;

import com.barbearia.barbearia.modules.googlecalender.dto.request.GoogleCalendarConnectRequest;
import com.barbearia.barbearia.modules.googlecalender.dto.response.GoogleCalendarAuthorizationUrlResponse;
import com.barbearia.barbearia.modules.googlecalender.dto.response.GoogleCalendarConnectionResponse;
import com.barbearia.barbearia.modules.googlecalender.service.GoogleCalenderService;
import com.barbearia.barbearia.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google-calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalenderService googleCalenderService;

    @GetMapping("/authorize-url")
    public ResponseEntity<GoogleCalendarAuthorizationUrlResponse> authorizeUrl(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String redirectUri) {
        return ResponseEntity.ok(googleCalenderService.createAuthorizationUrl(userDetails.user().getId(), redirectUri));
    }

    @PostMapping("/connect")
    public ResponseEntity<GoogleCalendarConnectionResponse> connect(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid GoogleCalendarConnectRequest request) {
        return ResponseEntity.ok(googleCalenderService.connectCurrentUser(userDetails.user().getId(), request));
    }

    @GetMapping("/status")
    public ResponseEntity<GoogleCalendarConnectionResponse> status(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(googleCalenderService.getStatus(userDetails.user().getId()));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        googleCalenderService.disconnectCurrentUser(userDetails.user().getId());
        return ResponseEntity.noContent().build();
    }
}


