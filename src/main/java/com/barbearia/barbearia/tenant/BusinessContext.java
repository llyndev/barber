package com.barbearia.barbearia.tenant;

import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.business.model.BusinessRole;

import java.util.Objects;
import java.util.Optional;

public final class BusinessContext {


    // Um único ThreadLocal com os dois dados juntos
    // Elimina a possibilidade de ter role sem businessId (ou vice-versa)
    private static final ThreadLocal<BusinessScope> CONTEXT = new ThreadLocal<>();

    private BusinessContext() {}


    // Record imutável: uma vez setado, ninguém altera parcialmente seu estado.
    public record BusinessScope(Long businessId, BusinessRole role) {
        public BusinessScope {
            Objects.requireNonNull(businessId, "businessId cannot be null");
        }
    }

    public static void set(Long businessId, BusinessRole role) {
        CONTEXT.set(new BusinessScope(businessId, role));
    }

    // Usar require nos repositories/services
    public static Long requireBusinessId() {
        BusinessScope scope = CONTEXT.get();

        if (scope == null) {
            throw new IllegalStateException(
                    "Business context not defined.");
        }
        return scope.businessId();
    }

    // Versão tolerante, para código que legitimamente funciona sem tenant
    public static Optional<Long> findBusinessId() {
        return Optional.ofNullable(CONTEXT.get()).map(BusinessScope::businessId);
    }

    public static Optional<BusinessRole> getRole() {
        return Optional.ofNullable(CONTEXT.get()).map(BusinessScope::role);
    }

    public static BusinessRole requireRole() {
        return getRole().orElseThrow(
                () -> new ResourceNotFoundException("Role not found.")
        );
    }

    private static final ThreadLocal<String> BUSINESS = new ThreadLocal<>();

    private static final ThreadLocal<String> BUSINESS_ROLE = new ThreadLocal<>();

    public static BusinessScope capture() {
        return CONTEXT.get();
    }

    public static void restore(BusinessScope scope) {
        if (scope == null) CONTEXT.remove();
        else CONTEXT.set(scope);
    }

    public static void clear() {
        CONTEXT.remove();
    }


}
