package com.barbearia.barbearia.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfigurationSource;

import com.barbearia.barbearia.tenant.ContextFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ContextFilter contextFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error", "/csrf").permitAll()
                        
                        // Public Authentication Endpoints
                        .requestMatchers("/auth/**", "/register", "/register/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // Liberar imagens
                        
                        // Public Business Information
                        .requestMatchers(HttpMethod.GET, "/business", "/business/{id}", "/business/slug/{slug}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/barber-service", "/barber-service/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/barbers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/opening-hours/status", "/opening-hours/weekly-schedule").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll()
                        
                        // Public Scheduling Information (Availability)
                        .requestMatchers(HttpMethod.GET, "/scheduling/available-times").permitAll()
                        
                        // Public Resources
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/leads").permitAll()

                        // Protected Endpoints
                        .requestMatchers("/scheduling/**").authenticated()
                        .requestMatchers("/business/**").authenticated()
                        .requestMatchers("/my-business/**").authenticated()
                        .requestMatchers("/my-invitations/**").authenticated()
                        .requestMatchers("/opening-hours/**").authenticated()
                        .requestMatchers("/api/v1/inventory/**").authenticated()
                        .requestMatchers("/barber-service/**").authenticated()
                        .requestMatchers("/orders/**").authenticated()
                        .requestMatchers("/expenses", "/expenses/**").authenticated()
                        
                        // Role Based
                        .requestMatchers("/users/**").hasRole("PLATFORM_ADMIN")
                        
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(contextFilter, JwtFilter.class);

        return http.build();

    }
}