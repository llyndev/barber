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

import com.barbearia.barbearia.tenant.ContextFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ContextFilter contextFilter;

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
                .cors(cors -> {})

                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**", "/register").permitAll()
                        .requestMatchers("/register/complete").permitAll()
                        .requestMatchers("/leads").permitAll()
                        .requestMatchers("/business", "/business/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/barber-service", "/barber-service/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/barbers").permitAll()
                        .requestMatchers("/scheduling/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/opening-hours/weekly-schedule").authenticated()

                        .requestMatchers("/my-business/**").authenticated()
                        .requestMatchers("/my-invitations/**").authenticated()

                        .requestMatchers("/scheduling/barber/").hasRole("BARBER")

                        .requestMatchers("/opening-hours/**").authenticated()
                        .requestMatchers("/api/v1/inventory" , "/api/v1/inventory/**").authenticated()
                        .requestMatchers("/barber-service/**").authenticated()

                        .requestMatchers("/users/**").hasRole("PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(contextFilter, JwtFilter.class);

        return http.build();

    }
}