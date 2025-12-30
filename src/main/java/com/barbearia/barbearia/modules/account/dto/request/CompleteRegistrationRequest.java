package com.barbearia.barbearia.modules.account.dto.request;

public record CompleteRegistrationRequest(
    String token,
    String password,
    String confirmPassword,
    String document
) {
    
}
