package com.barbearia.barbearia.modules.catalog.repository;

import com.barbearia.barbearia.modules.catalog.model.BarberService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BarberServiceRepository extends JpaRepository<BarberService, Long> {

    List<BarberService> findAllByBusinessIdAndActiveTrue(Long businessId);

    List<BarberService> findAllByBusinessSlugAndActiveTrue(String slug);

    List<BarberService> findAllByBusinessId(Long businessId);

    Optional<BarberService> findByIdAndBusinessId(Long id, Long businessId);

    List<BarberService> findAllByIdInAndBusinessId(Set<Long> id, Long businessId);
}
