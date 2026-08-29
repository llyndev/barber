package com.barbearia.barbearia.modules.catalog.controller;

import com.barbearia.barbearia.modules.catalog.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.modules.catalog.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.catalog.service.BarberServiceService;
import com.barbearia.barbearia.modules.business.service.BusinessService;
import jakarta.validation.Valid;
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
        return ResponseEntity.ok(barberServiceService.findBusinessBySlug(slug));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(barberServiceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BarberServiceResponse> create(@Valid @RequestBody BarberServiceRequest barberServiceRequest) {
        BarberServiceResponse response = barberServiceService.save(barberServiceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        barberServiceService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BarberServiceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BarberServiceRequest request) {
        return ResponseEntity.ok(barberServiceService.update(id, request));
    }
}
