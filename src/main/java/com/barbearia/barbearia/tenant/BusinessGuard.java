package com.barbearia.barbearia.tenant;

import com.barbearia.barbearia.modules.business.model.BusinessRole;
import org.springframework.stereotype.Component;

@Component
public class BusinessGuard {

    public void requireOwnerOrManager() {
        BusinessRole role = BusinessContext.requireRole();
        if (role != BusinessRole.OWNER && role != BusinessRole.MANAGER) {
            throw new SecurityException("Unauthorized.");
        }
    }

    public void requireOwner() {
        BusinessRole role = BusinessContext.requireRole();
        if (role != BusinessRole.OWNER) {
            throw new SecurityException("Unahtorized.");
        }
    }
}
