package com.barbearia.barbearia.modules.leads.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.barbearia.barbearia.modules.leads.dto.request.LeadRequest;
import com.barbearia.barbearia.modules.leads.model.Lead;
import com.barbearia.barbearia.modules.leads.service.LeadService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public void captureLeads(@RequestBody LeadRequest request) {
        leadService.captureLeads(request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, String>> approveLead(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam(defaultValue = "30") int days) {
        String token = leadService.approveLead(id, days);
        String link = "http://localhost:3000/invite/token=" + token;
        return ResponseEntity.ok(Map.of(
            "message", "Lead aprovado com sucesso! Envie o link abaixo para o cliente.",
            "registration_link", link
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<Lead>> getLeads(Lead lead) {
        return ResponseEntity.ok(leadService.getLeads(lead));
    }

}
