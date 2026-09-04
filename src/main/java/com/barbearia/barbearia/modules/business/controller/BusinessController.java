package com.barbearia.barbearia.modules.business.controller;

import java.time.Duration;
import java.util.List;

import com.barbearia.barbearia.modules.business.dto.response.BusinessSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
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

    // Busca todas as barbearias ativas e inativas para o administrador da plataforma
    @GetMapping("/admin/business")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Page<BusinessSummaryResponse>> searchAll(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<BusinessSummaryResponse> result = businessService.searchBusinesses(q, true, pageable);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(result);
    }


    // Busca barbearias por nome, cidade e bairro
    @GetMapping
    public ResponseEntity<Page<BusinessSummaryResponse>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        Page<BusinessSummaryResponse> result = businessService.searchBusinesses(q, false, pageable);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(result);
    }

    @GetMapping("/my-businesses")
    public ResponseEntity<List<BusinessResponse>> listMyBusinesses(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<BusinessResponse> business = businessService.findAllByOwnerId(userDetails.id());
        return ResponseEntity.ok(business);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<BusinessResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(businessService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> create(
            @RequestBody @Valid BusinessRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        BusinessResponse response = businessService.create(request, userDetails.id());

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
