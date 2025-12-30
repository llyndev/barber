package com.barbearia.barbearia.modules.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.barbearia.barbearia.modules.inventory.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByBusinessIdAndActiveTrue(Long businessId);

    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);

    @Query("SELECT p FROM Product p WHERE p.business.id = :businessId AND p.quantity <= p.minQuantity AND p.active = true")
    List<Product> findLowStockProducts(@Param("businessId") Long businessId);
    
}
