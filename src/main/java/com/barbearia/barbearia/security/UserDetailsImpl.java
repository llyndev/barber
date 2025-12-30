package com.barbearia.barbearia.security;

import com.barbearia.barbearia.modules.account.model.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record UserDetailsImpl(AppUser user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getPlatformRole().name()));
    }

    @Override
    public String getPassword(){
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired(){
        if (user.getDateExpirationAccount() == null) {
            return true; // Sem data definida, não expira
        }
        return user.getDateExpirationAccount().isAfter(java.time.LocalDate.now());
    }

    @Override
    public boolean isAccountNonLocked(){
        return !this.user.isBlocked();
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

    @Override
    public boolean isEnabled(){
        return this.user.isActive();
    }


}
