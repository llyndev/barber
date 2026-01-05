package com.barbearia.barbearia.modules.scheduling.model;

import com.barbearia.barbearia.modules.catalog.model.BarberService;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.Business;
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

    @Column(name = "client_name")
    private String clientName;

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

    @Builder.Default
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

    @OneToMany(mappedBy = "scheduling", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchedulingProduct> productsUsed;

    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;

}
