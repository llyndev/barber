package com.barbearia.barbearia.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "scheduling")
public class Scheduling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne()
    @JoinColumn(name = "barber_id")
    private AppUser barber;

    @ManyToMany
    @JoinTable(
            name = "scheduling_services",
            joinColumns = @JoinColumn(name = "scheduling_id"),
            inverseJoinColumns = @JoinColumn(name = "barber_service_id"))
    private List<BarberService> barberService;

    @Column(name = "scheduling_date_time")
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus states = AppointmentStatus.SCHEDULED;

    @Column(name = "reason_cancel")
    private String reasonCancel;

    @Column(name = "additional_value")
    private BigDecimal additionalValue;

    @Column(name = "observation_scheduling")
    private String observation;

    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

}
