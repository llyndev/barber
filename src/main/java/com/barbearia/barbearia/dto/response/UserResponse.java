package com.barbearia.barbearia.dto.response;

import com.barbearia.barbearia.model.AppUser;

public record UserResponse(Long id, String name, String email, String telephone, AppUser.Role role){}