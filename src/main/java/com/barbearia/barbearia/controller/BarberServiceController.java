package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.model.Business;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.service.BarberServiceService;
import com.barbearia.barbearia.service.BusinessService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/barber-service")
public class BarberServiceController {

    private final BarberServiceService barberServiceService;
    private final BusinessService businessService;

    @GetMapping
    public List<BarberServiceResponse> getAll() {
        return barberServiceService.listAll();
    }

    @GetMapping("/business/{slug}")
    public ResponseEntity<List<BarberServiceResponse>> getByBusinessSlug(@PathVariable String slug) {
        // Busca a barbearia pelo slug
        Business business = businessService.getBusinessBySlug(slug);
        
        // Retorna os serviços da barbearia
        List<BarberServiceResponse> services = barberServiceService.findAllByBusinessId(business.getId());
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(barberServiceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BarberServiceResponse> create(
            @RequestBody BarberServiceRequest barberServiceRequest, 
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerBySlug(businessSlug, userDetails.user().getId());
        }

        BarberServiceResponse response = barberServiceService.save(barberServiceRequest, userDetails);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerBySlug(businessSlug, userDetails.user().getId());
        }
        
        barberServiceService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BarberServiceResponse> update(
            @PathVariable Long id, 
            @RequestBody BarberServiceRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "X-Business-Slug", required = false) String businessSlug) {
        
        if (businessSlug != null && !businessSlug.isBlank()) {
            businessService.validateOwnerBySlug(businessSlug, userDetails.user().getId());
        }
        
        return ResponseEntity.ok(barberServiceService.update(id, request));
    }
}
