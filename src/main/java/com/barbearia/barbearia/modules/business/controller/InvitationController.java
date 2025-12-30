package com.barbearia.barbearia.modules.business.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.barbearia.barbearia.modules.business.dto.request.AddUserToBusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.InvitationResponse;
import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import com.barbearia.barbearia.modules.business.service.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class InvitationController {

    
    private final InvitationService invitationService;
    private final BusinessService businessService;

    @PostMapping("/my-business/invitations")
    public ResponseEntity<InvitationResponse> createInvitation(
            @Valid @RequestBody AddUserToBusinessRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerBySlug(businessSlug, userDetails.user().getId());
        }
        
        InvitationResponse invitationResponse = invitationService.createInvitation(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(invitationResponse);
    }

    @GetMapping("/my-invitations")
    public ResponseEntity<List<InvitationResponse>> getMyPendingInvitations(@AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<InvitationResponse> invitations = invitationService.getMyPendingInvitations(userDetails);
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/my-invitations/{id}/accept")
    public ResponseEntity<UserBusinessResponse> acceptInvitation(
            @PathVariable("id") Long invitationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UserBusinessResponse link = invitationService.acceptInvitation(invitationId, userDetails);

        return ResponseEntity.ok(link);
    }

    @PostMapping("/my-invitations/{id}/decline")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable("id") Long invitationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        invitationService.declineInvitation(invitationId, userDetails);
        return ResponseEntity.noContent().build();
    }
}

