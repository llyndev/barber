package com.barbearia.barbearia.tenant;

public final class TenantContext {

    private static final ThreadLocal<TenantInfo> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentBusinessId(TenantInfo tenant) {
        TENANT.set(tenant);
    }

    public static TenantInfo getTenant() {
        return TENANT.get();
    }

    public static String getTenantId() {
        TenantInfo t = TENANT.get();
        return t != null ? t.getTenantId() : null; 
    }

    public static void clear() {
        TENANT.remove();
    }
}
