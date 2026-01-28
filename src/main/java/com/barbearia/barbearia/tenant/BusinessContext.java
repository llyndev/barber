package com.barbearia.barbearia.tenant;

public final class BusinessContext {

    private static final ThreadLocal<String> BUSINESS = new ThreadLocal<>();

    private static final ThreadLocal<String> BUSINESS_ROLE = new ThreadLocal<>();

    private BusinessContext() {}

    public static void setBusinessId(String businessId) {
        BUSINESS.set(businessId);
    }

    public static String getBusinessId() {
        return BUSINESS.get();
    }

    public static void setBusinessRole(String role) {
        BUSINESS_ROLE.set(role);
    }

    public static String getBusinessRole() {
        return BUSINESS_ROLE.get();
    }

    public static void clear() {
        BUSINESS.remove();
        BUSINESS_ROLE.remove();
    }


}
