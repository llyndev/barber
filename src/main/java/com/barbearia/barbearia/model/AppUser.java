package com.barbearia.barbearia.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
    private Role role;

    private boolean active = true;
    private boolean blocked = false;
    private LocalDate dateExpirationAccount;

    public enum Role {
        ADMIN,
        BARBER,
        CLIENT
    }
}
