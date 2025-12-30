package com.barbearia.barbearia.modules.business.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import com.barbearia.barbearia.modules.business.service.UserBusinessService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/my-business")
@RequiredArgsConstructor
public class UserBusinessController {

    private final UserBusinessService userBusinessService;
    private final BusinessService businessService;

    @GetMapping("/users")
    public ResponseEntity<List<UserBusinessResponse>> listUsersInMyBusiness(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerOrManagerBySlug(businessSlug, userDetails.user().getId());
        }
        
        List<UserBusinessResponse> users = userBusinessService.listUsersInMyBusiness();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> removeUserFromMyBusiness(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerBySlug(businessSlug, userDetails.user().getId());
        }
        
        userBusinessService.removeUserFromMyBusiness(userId);
        return ResponseEntity.noContent().build();
    }
}
