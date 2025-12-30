package com.barbearia.barbearia.modules.common.address.service;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.barbearia.barbearia.modules.common.address.dto.response.AddressResponse;
import com.barbearia.barbearia.exception.CepNotFoundException;
import com.barbearia.barbearia.exception.ExternalServiceException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final WebClient viaCepWebClient;

    public AddressResponse getCep(String cep) {
        if (cep == null || cep.trim().isEmpty()) {
            throw new CepNotFoundException("CEP inválido");
        }

        String sanitizedCep = cep.replaceAll("\\D", "");
        if (sanitizedCep.length() != 8) {
            throw new CepNotFoundException("CEP inválido:" + cep);
        }

        AddressResponse addressResponse = viaCepWebClient.get()
                .uri("/{cep}/json/", sanitizedCep)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ExternalServiceException(
                                "Erro ao consultar CEP: " + response.statusCode()
                        ))
                )
                .bodyToMono(AddressResponse.class)
                .block();

        if (addressResponse == null || addressResponse.erro()) {
            throw new CepNotFoundException("Erro ao consultar CEP: " + cep);
        }

        return addressResponse;
    }

}
