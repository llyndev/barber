package com.barbearia.barbearia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barbearia.barbearia.dto.request.BusinessRequest;
import com.barbearia.barbearia.dto.response.BusinessResponse;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.service.BusinessService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    public ResponseEntity<List<BusinessResponse>> listAll(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(businessService.searchBusinesses(search));
        }
        return ResponseEntity.ok(businessService.getAll());
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

}
