package com.barbearia.barbearia.modules.common.address.controller;

import com.barbearia.barbearia.modules.common.address.dto.response.CepLookupResponse;
import com.barbearia.barbearia.modules.common.address.service.AddressService;
import com.barbearia.barbearia.security.ratelimit.ClientIpResolver;
import com.barbearia.barbearia.security.ratelimit.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cep")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final RateLimiterService rateLimiter;

    @GetMapping("/{cep}")
    public CepLookupResponse lookup (@PathVariable String cep,
                                     HttpServletRequest request) {
        rateLimiter.consume(ClientIpResolver.resolve(request), RateLimiterService.LimitType.CEP_LOOKUP);

        return CepLookupResponse.fromViaCep(addressService.getCep(cep));
    }

}
