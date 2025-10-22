package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.service.BarberServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/barber-service")
public class BarberServiceController {

    private final BarberServiceService barberServiceService;

    @GetMapping
    public List<BarberServiceResponse> getAll() {
        return barberServiceService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(barberServiceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BarberServiceResponse> create(@RequestBody BarberServiceRequest barberServiceRequest) {
        return ResponseEntity.ok(barberServiceService.save(barberServiceRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        barberServiceService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BarberServiceResponse> update(@PathVariable Long id, @RequestBody BarberServiceRequest request) {
        return ResponseEntity.ok(barberServiceService.update(id, request));
    }
}
