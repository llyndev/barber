package com.barbearia.barbearia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;


@Entity
@Table(name = "business")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(nullable = false, length = 120, unique = true)
    private String slug; // subdominio

    @Embedded
    private Address address;

    @Column(length = 64)
    private String timezone;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime planExpirationDate;

    @Builder.Default
    private Instant createdAt = Instant.now();

}
