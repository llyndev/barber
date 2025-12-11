package com.barbearia.barbearia.repository;

import com.barbearia.barbearia.model.BarberService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarberServiceRepository extends JpaRepository<BarberService, Long> {

    List<BarberService> findAllByBusinessIdAndActiveTrue(Long businessId);

    Optional<BarberService> findByIdAndBusinessId(Long id, Long businessId);
}
