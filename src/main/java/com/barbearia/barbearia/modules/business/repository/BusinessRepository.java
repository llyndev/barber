package com.barbearia.barbearia.modules.business.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.business.model.Business;

public interface BusinessRepository extends JpaRepository<Business, Long>{

    Optional<Business> findBySlug(String slug);

    Optional<Business> findByOwnerId(Long id);

}
