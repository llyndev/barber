package com.barbearia.barbearia.modules.orders.repository;

import com.barbearia.barbearia.modules.orders.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
