package com.barbearia.barbearia.modules.inventory.repository;

import java.util.List;
import java.util.Optional;

import com.barbearia.barbearia.modules.inventory.dto.response.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.inventory.model.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = {"product", "performedBy", "performedBy.user"})
    Page<StockMovement> findByProduct_Business_IdOrderByOccurredAtDesc(Long businessId, Pageable pageable);

    Page<StockMovement> findByProductIdOrderByOccurredAtDesc(Long productId, Pageable pageable);
}
