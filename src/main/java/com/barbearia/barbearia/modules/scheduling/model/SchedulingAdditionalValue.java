package com.barbearia.barbearia.modules.scheduling.model;

import com.barbearia.barbearia.modules.account.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "scheduling_additional_value")
public class SchedulingAdditionalValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduling_id", nullable = false)
    private Scheduling scheduling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private AppUser barber;

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;
}
