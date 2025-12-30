package com.barbearia.barbearia.modules.catalog.model;

import com.barbearia.barbearia.modules.business.model.Business;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "barber_service", indexes = {
        @Index(name = "idx_service_business", columnList = "business_id")
})
public class BarberService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_service")
    private String nameService;

    @Column(name = "description_service")
    private String description;

    @Column(name = "duration_in_minutes")
    private Integer durationInMinutes;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "price_service", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "active")
    private boolean active;
}
