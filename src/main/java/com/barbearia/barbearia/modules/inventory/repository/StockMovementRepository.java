package com.barbearia.barbearia.modules.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.inventory.model.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByDateDesc(Long productId);

    List<StockMovement> findByProduct_Business_IdOrderByDateDesc(Long businessId);
}
