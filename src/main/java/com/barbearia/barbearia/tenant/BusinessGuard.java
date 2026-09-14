package com.barbearia.barbearia.tenant;

import com.barbearia.barbearia.modules.business.model.BusinessRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class BusinessGuard {

    public void requireOwnerOrMangerOrBarber() {
        if (!isOwnerOrManagerOrBarber()) {
            throw new AccessDeniedException("Acesso negado.");
        }
    }

    public void requireOwnerOrManager() {
        if (!isOwnerOrManager()) {
            throw new AccessDeniedException("Acesso negado.");
        }
    }

    public void requireOwner() {
        if (!isOwner()) {
            throw new AccessDeniedException("Acesso negado.");
        }
    }

    public boolean isOwnerOrManager() {
        return BusinessContext.getRole()
                .filter(r -> r == BusinessRole.OWNER || r == BusinessRole.MANAGER)
                .isPresent();
    }

    public boolean isOwnerOrManagerOrBarber() {
        return BusinessContext.getRole()
                .filter(r -> r == BusinessRole.OWNER || r == BusinessRole.MANAGER || r == BusinessRole.BARBER)
                .isPresent();
    }

    public boolean isBarber() {
        return BusinessContext.getRole()
                .filter(r -> r == BusinessRole.BARBER)
                .isPresent();
    }

    public boolean isOwner() {
        return BusinessContext.getRole()
                .filter(r -> r == BusinessRole.OWNER)
                .isPresent();
    }
}
