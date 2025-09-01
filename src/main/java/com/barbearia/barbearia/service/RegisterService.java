package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.RegisterRequest;
import org.springframework.stereotype.Service;

@Service
public interface RegisterService {

    void registerUser(RegisterRequest request);
}
