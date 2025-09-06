package com.barbearia.barbearia.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "barber_service")
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

    @Column(name = "price_service")
    private BigDecimal price;
}
