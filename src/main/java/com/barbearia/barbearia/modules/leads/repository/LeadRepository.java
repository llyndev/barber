package com.barbearia.barbearia.modules.leads.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barbearia.barbearia.modules.leads.model.Lead;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>{

    Optional<Lead> findByRegistrationToken(String token);

}
