package com.barbearia.barbearia.security;

import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.model.PlatformRole;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


public record UserDetailsImpl(
        Long id,
        String email,
        String passwordHash,
        PlatformRole platformRole,
        boolean active,
        boolean blocked,
        LocalDate expirationDate,
        Collection<GrantedAuthority> authorities
) implements UserDetails, CredentialsContainer {

    // Valida invariantes
    public UserDetailsImpl {
        Objects.requireNonNull(id, "User ID is required.");
        Objects.requireNonNull(platformRole, "Role is required.");
        authorities = List.copyOf(authorities);
    }

    public static UserDetailsImpl from(AppUser user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getPlatformRole(),
                user.isActive(),
                user.isBlocked(),
                user.getDateExpirationAccount(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getPlatformRole().name()))
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(platformRole.authority()));
    }

    @Override
    public String getPassword(){
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public void eraseCredentials() {}

    public UserDetailsImpl withoutCredentials() {
        return new UserDetailsImpl(id, email, null, platformRole,
                active, blocked, expirationDate, authorities);
    }

    @Override
    public boolean isAccountNonExpired(){
        if (expirationDate == null) {
            return true;
        }

        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        return !expirationDate.isBefore(today);
    }

    @Override
    public boolean isAccountNonLocked(){
        return !blocked;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return active;
    }

    @Override
    public String toString(){
        return "UserDetailsImpl[id=" + id + ", email=" + email + "]";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UserDetailsImpl other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }


}
