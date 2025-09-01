package com.barbearia.barbearia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "barber_id")
    private AppUser barber;

    @ManyToOne
    @JoinColumn(name = "barber_service_id")
    private BarberService barberService;

    @Column(name = "scheduling_date_time")
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus states = AppointmentStatus.SCHEDULED;

}
