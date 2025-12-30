package com.barbearia.barbearia.modules.account.dto.request;

public record UserRequest(
        String name,
        String email,
        String telephone,
        String password,
        String confirmPassword) {}
