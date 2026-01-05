package com.barbearia.barbearia.modules.orders.repository;

import com.barbearia.barbearia.modules.orders.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByBusinessId(Long businessId);

    Optional<Order> findBySchedulingId(Long schedulingId);

    List<Order> findByBusinessIdOrderByCreatedAtDesc(Long businessId);
}
