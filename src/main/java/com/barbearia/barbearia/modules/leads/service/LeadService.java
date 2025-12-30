package com.barbearia.barbearia.modules.leads.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.barbearia.barbearia.modules.leads.dto.request.LeadRequest;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.leads.model.Lead;
import com.barbearia.barbearia.modules.leads.model.LeadStatus;
import com.barbearia.barbearia.modules.leads.repository.LeadRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;

    public List<Lead> getLeads(Lead lead) {
        return leadRepository.findAll();
    }
    
    public void captureLeads(LeadRequest lead) {
        if (lead.email() == null || lead.email().isEmpty()) {
            throw new InvalidRequestException("Email is required.");
        }

        if (lead.telephone() == null || lead.telephone().isEmpty()) {
            throw new InvalidRequestException("Telephone number is required.");
        }

        Lead leads = Lead.builder()
                .name(lead.name())
                .email(lead.email())
                .telephone(lead.telephone())
                .businessName(lead.businessName())
                .plan(lead.plan())
                .status(LeadStatus.PENDING)
                .build();
        leadRepository.save(leads);
    } 

    @Transactional
    public String approveLead(Long leadId, int expirationDays) {
        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        String token = UUID.randomUUID().toString();
        lead.setRegistrationToken(token);
        lead.setTokenExpiration(LocalDateTime.now().plusHours(48));
        lead.setStatus(LeadStatus.APPROVED);
        lead.setAccountExpirationDate(java.time.LocalDate.now().plusDays(expirationDays));

        leadRepository.save(lead);

        return token;
    }

}
