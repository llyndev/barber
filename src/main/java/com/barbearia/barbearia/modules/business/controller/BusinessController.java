package com.barbearia.barbearia.modules.business.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barbearia.barbearia.modules.business.dto.request.BusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.BusinessResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.business.service.BusinessService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    public ResponseEntity<List<BusinessResponse>> listAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(businessService.searchBusinesses(search, includeInactive));
        }
        return ResponseEntity.ok(businessService.getAll(includeInactive));
    }

    @GetMapping("/my-businesses")
    public ResponseEntity<List<BusinessResponse>> listMyBusinesses(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<BusinessResponse> business = businessService.findAllByOwnerId(userDetails.user().getId());
        return ResponseEntity.ok(business);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(businessService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<BusinessResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(businessService.getBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> create(
            @Valid @RequestBody BusinessRequest request, 
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BusinessResponse response = businessService.create(request, userDetails.user());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BusinessRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BusinessResponse response = businessService.update(id, request, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<BusinessResponse> deactivate(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        BusinessResponse response = businessService.deactivateBusiness(slug, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{slug}/activate")
    public ResponseEntity<BusinessResponse> activate(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        BusinessResponse response = businessService.activateBusiness(slug, userDetails.user());
        return ResponseEntity.ok(response);
    }
}
