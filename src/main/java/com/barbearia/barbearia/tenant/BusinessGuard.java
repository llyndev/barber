package com.barbearia.barbearia.tenant;

import com.barbearia.barbearia.modules.business.model.BusinessRole;
import org.springframework.stereotype.Component;

@Component
public class BusinessGuard {

    public void requireOwnerOrMangerOrBarber() {
        BusinessRole role = BusinessContext.requireRole();
        if (role != BusinessRole.OWNER && role != BusinessRole.MANAGER && role != BusinessRole.BARBER) {
            throw new SecurityException("Unauthorized.");
        }
    }

    public void requireOwnerOrManager() {
        BusinessRole role = BusinessContext.requireRole();
        if (role != BusinessRole.OWNER && role != BusinessRole.MANAGER) {
            throw new SecurityException("Unauthorized.");
        }
    }

    public void requireOwner() {
        BusinessRole role = BusinessContext.requireRole();
        if (role != BusinessRole.OWNER) {
            throw new SecurityException("Unauthorized.");
        }
    }

    public boolean isOwnerOrManager() {
        BusinessRole role = BusinessContext.requireRole();
        return role == BusinessRole.OWNER || role == BusinessRole.MANAGER;
    }

    public boolean isOwnerOrManagerOrBarber() {
        BusinessRole role = BusinessContext.requireRole();
        return role == BusinessRole.OWNER || role == BusinessRole.MANAGER || role == BusinessRole.BARBER;
    }

    public boolean isBarber() {
        BusinessRole role = BusinessContext.requireRole();
        return role == BusinessRole.BARBER;
    }
}
