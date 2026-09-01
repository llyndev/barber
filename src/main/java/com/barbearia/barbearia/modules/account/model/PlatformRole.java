package com.barbearia.barbearia.modules.account.model;

public enum PlatformRole{
    CLIENT,
    BUSINESS_OWNER,
    SUPPORT,
    PLATFORM_ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}