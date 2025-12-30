package com.barbearia.barbearia.modules.leads.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "lead_name", nullable = false)
    String name;

    @Column(name = "lead_business_name", nullable = false)
    String businessName;

    @Column(name = "lead_email", nullable = false)
    String email;

    @Column(name = "lead_telephone", nullable = false)
    String telephone;

    @Column(name = "lead_plan", nullable = false)
    String plan;

    private String document;

    @Column(name = "registration_token", unique = true)
    private String registrationToken;

    private LocalDateTime tokenExpiration;

    @Enumerated(EnumType.STRING)
    private LeadStatus status;

    private java.time.LocalDate accountExpirationDate;

}
