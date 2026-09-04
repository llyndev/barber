package com.barbearia.barbearia.config;

import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.security.AppUserDetailsService;
import com.barbearia.barbearia.security.JwtFilter;
import com.barbearia.barbearia.security.JwtUtil;
import com.barbearia.barbearia.tenant.ContextFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Cria os filtros de segurança como beans e IMPEDE que o Spring Boot os
// registre automaticamente no chain do servlet container.
@Configuration
public class FilterConfig {

    @Bean
    public JwtFilter jwtFilter(JwtUtil jwtUtil,
                               AppUserDetailsService appUserDetailsService) {
        return new JwtFilter(jwtUtil, appUserDetailsService);
    }

    @Bean
    public ContextFilter contextFilter(BusinessRepository businessRepository,
                                       UserBusinessRepository userBusinessRepository,
                                       ObjectMapper objectMapper) {
        return new ContextFilter(businessRepository, userBusinessRepository, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> disableJwtFilterAutoRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ContextFilter> disableContextFilterAutoRegistration(ContextFilter filter) {
        FilterRegistrationBean<ContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }


}
