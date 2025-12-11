package com.barbearia.barbearia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.model.Business;

public interface BusinessRepository extends JpaRepository<Business, Long>{

    Optional<Business> findBySlug(String slug);

}
