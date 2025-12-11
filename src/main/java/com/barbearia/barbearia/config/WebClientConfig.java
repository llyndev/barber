package com.barbearia.barbearia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {


    @Value("${clients.viacep.base-url}")
    private String viaCepBaseUrl;

    @Bean
    public WebClient viaCepWebClient() {
        return WebClient.builder()
                .baseUrl(viaCepBaseUrl)
                .build();
    }
}
