package com.barbearia.barbearia.modules.business.model;

import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.common.address.model.Address;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;


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

    @Column(name = "telephone")
    private String telephone;

    @Column(nullable = false, length = 120, unique = true)
    private String slug; // subdominio

    @Column(name = "amenities")
    private List<String> amenities;

    @Embedded
    private Address address;

    @Column(length = 64)
    private String timezone;

    @Builder.Default
    private boolean active = true;

    @Column(name = "instagram_link")
    private String instagramLink;

    @Column(nullable = true)
    private String businessImage;

    @Column(nullable = true)
    private String backgroundImage;

    private LocalDateTime planExpirationDate;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

}
