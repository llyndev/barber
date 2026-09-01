package com.barbearia.barbearia.tenant;

import com.barbearia.barbearia.modules.account.model.PlatformRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class PlatformContext {

    private PlatformContext() {}

    public static Optional<PlatformRole> currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .map(PlatformRole::valueOf)
                .findFirst();
    }

    public static boolean has(PlatformRole role) {
        return currentRole().filter(role::equals).isPresent();
    }

    public static void require(PlatformRole role) {
        if (!has(role)) {
            throw new AccessDeniedException("Require platform role: " + role);
        }
    }

}
