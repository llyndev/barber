package com.barbearia.barbearia.modules.common.address.service;

import com.barbearia.barbearia.exception.InvalidRequestException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;

import com.barbearia.barbearia.modules.common.address.dto.response.AddressResponse;
import com.barbearia.barbearia.exception.CepNotFoundException;
import com.barbearia.barbearia.exception.ExternalServiceException;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final RestClient viaCepClient;

    private final Cache<String, AddressResponse> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofDays(30))
            .build();

    public AddressService(RestClient.Builder builder,
                          @Value("${viacep.base-url:https://viacep.com.br/ws}") String baseUrl) {

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.viaCepClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

    }

    public AddressResponse getCep(String rawCep) {
        String cep = sanitize(rawCep);

        return cache.get(cep, this::fetchFromViaCep);

    }

    private String sanitize(String rawCep) {
        if (rawCep == null || rawCep.isBlank()) {
            throw new InvalidRequestException("Zip code not provided.");
        }

        String digits = rawCep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new InvalidRequestException("Zip code must 8 digits" + rawCep);
        }

        return digits;
    }

    private AddressResponse fetchFromViaCep(String cep) {
        try {
            AddressResponse response = viaCepClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new CepNotFoundException("Zip code not found: " + cep);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ExternalServiceException(
                                "Zip code service unavaiable. Please try again later."
                        );
                    })
                    .body(AddressResponse.class);

            if (response == null || response.erro()) {
                throw new CepNotFoundException("Zip code not found: " + cep);
            }

            return response;
        } catch (ResourceAccessException ex) {
            log.warn("Falha ao consultar ViaCEP para {}: {}", cep, ex.getMessage());
            throw new ExternalServiceException(
                    "Não foi possível consultar o CEP no momento.");
        }
    }
}
