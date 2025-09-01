package com.barbearia.barbearia.dto.request;

public record UserRequest(String name, String email, String telephone, String password, String confirmPassword) {}
