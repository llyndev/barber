package com.barbearia.barbearia.modules.business.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.barbearia.barbearia.exception.ExternalServiceException;
import com.barbearia.barbearia.modules.account.dto.response.BusinessPublicResponse;
import com.barbearia.barbearia.modules.business.dto.response.BusinessSummaryResponse;
import com.barbearia.barbearia.modules.business.model.BusinessImageType;
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
import org.springframework.web.multipart.MultipartFile;

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
                .cacheControl(CacheControl.noCache())
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
    public List<BusinessSummaryResponse> listMyBusinesses(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return businessService.findAllByOwnerId(userDetails.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public BusinessResponse getById(@PathVariable Long id) {
        return businessService.getById(id);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BusinessPublicResponse> getPublicBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(businessService.getPublicBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> create(
            @RequestBody @Valid BusinessRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        BusinessResponse response = businessService.create(request, userDetails.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public BusinessResponse update(@RequestBody @Valid BusinessRequest request) {
        return businessService.update(request);
    }

    @DeleteMapping
    public BusinessResponse deactivate() {
        return businessService.deactivate();
    }

    @PutMapping("/{id}/activate")
    public BusinessResponse activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return businessService.activate(id, userDetails.id());
    }

    @PutMapping("/image")
    public Map<String, String> updateImage(@RequestParam BusinessImageType type,
                                           @RequestParam("file") MultipartFile file) {
        try {
            String fileName = businessService.updateBusinessImage(type, file);
            return Map.of("fileNama", fileName);
        } catch (IOException ex) {
            throw new ExternalServiceException("Failed to save the image. Try again.");
        }
    }

    @DeleteMapping("/image")
    public ResponseEntity<Void> removeImage(@RequestParam BusinessImageType type) {
        businessService.removeBusinessImage(type);
        return ResponseEntity.noContent().build();
    }

}
