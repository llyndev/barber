package com.barbearia.barbearia.modules.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.barbearia.barbearia.modules.business.model.Business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Table(name = "product", indexes = {
    @Index(name = "idx_product_business", columnList = "business_id")
})
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    // Cod de barras
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "min_quantity")
    private Integer minQuantity;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice; // preço de custo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
