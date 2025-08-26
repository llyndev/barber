package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.model.BarberService;
import com.barbearia.barbearia.service.BarberServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/barber-service")
public class BarberServiceController {

    private final BarberServiceService barberServiceService;

    @GetMapping
    public List<BarberServiceResponse> getAll() {
        return barberServiceService.findAll();
    }

    @PostMapping
    public ResponseEntity<BarberServiceResponse> create(@RequestBody BarberServiceRequest barberServiceRequest) {
        return ResponseEntity.ok(barberServiceService.save(barberServiceRequest));
    }
}
