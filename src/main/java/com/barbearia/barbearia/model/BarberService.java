package com.barbearia.barbearia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BarberService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_service")
    private String nameService;

    @Column(name = "description_service")
    private String description;

    @Column(name = "duration_in_minutes")
    private int durationInMinutes;

    @Column(name = "price_service")
    private BigDecimal price;
}
