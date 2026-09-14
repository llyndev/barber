package com.barbearia.barbearia.modules.account.controller;

import com.barbearia.barbearia.modules.account.dto.request.CompleteRegistrationRequest;
import com.barbearia.barbearia.modules.account.dto.request.RegisterRequest;
import com.barbearia.barbearia.modules.account.dto.request.TokenPair;
import com.barbearia.barbearia.modules.account.dto.response.UserResponse;
import com.barbearia.barbearia.modules.account.mapper.UserMapper;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.service.RefreshTokenService;
import com.barbearia.barbearia.modules.account.service.RegisterService;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.security.ratelimit.ClientIpResolver;
import com.barbearia.barbearia.security.ratelimit.RateLimiterService;
import com.barbearia.barbearia.security.ratelimit.RateLimiterService.LimitType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;
    private final RateLimiterService rateLimiter;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                           HttpServletRequest httpRequest) {

        rateLimiter.consume(ClientIpResolver.resolve(httpRequest), LimitType.REGISTER);

        AppUser user = registerService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.toDTO(user));
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> completeRegistration(@RequestBody CompleteRegistrationRequest request) {
        registerService.completeRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
