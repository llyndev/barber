package com.barbearia.barbearia.modules.business.model;

import com.barbearia.barbearia.modules.account.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_business",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_business", columnNames = {"user_id", "business_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessRole role;

}
