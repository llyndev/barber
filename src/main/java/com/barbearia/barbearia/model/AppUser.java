package com.barbearia.barbearia.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class AppUser{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "users_name", nullable = false)
    private String name;

    @Column(name = "users_email", unique = true, nullable = false)
    private String email;

    @Column(name = "users_telephone", nullable = false)
    private String telephone;

    @Column(name = "users_password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "users_role")
    private PlatformRole platformRole;

    private String plantType;

    @Builder.Default
    @Column(nullable = true)
    private boolean isBusinessCreator = false;

    @Builder.Default
    private boolean active = true;
    private boolean blocked = false;
    private LocalDate dateExpirationAccount;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private List<UserBusiness> userBusinesses;

    public enum PlatformRole {
        CLIENT,
        BUSINESS_OWNER,
        PLATFORM_ADMIN
    }
}
