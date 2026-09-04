package com.barbearia.barbearia.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final AppUserDetailsService appUserDetailsService;

    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {

            // Valida assinatura e expiração
            Claims claims = jwtUtil.parseAccessToken(token);
            Long userId = jwtUtil.extractUserId(claims);

            // Impede que um refresh token (mais longevo) seja usado como access token
            if (!"access".equals(claims.get("type", String.class))) {
                throw new JwtException("Token type invalid.");
            }

            UserDetails userDetails = appUserDetailsService.loadUserById(userId);
            userDetailsChecker.check(userDetails);

            // Valida bloqueio, desativação e expiração de conta em todas as requisições
            userDetailsChecker.check(userDetails);

            var authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        } catch (ExpiredJwtException ex) {
            log.debug("Token expired for: {}", request.getRequestURI());
            unauthorized(response, "Token expirado", "TOKEN_EXPIRED");
            return;
        } catch (UsernameNotFoundException ex) {
            log.warn("Token expired for non-existent user: {}", ex.getMessage());
            unauthorized(response, "User not found", "USER_NOT_FOUND");
            return;
        } catch (AccountStatusException ex) {
            log.warn("Access denied due to account status: {}", ex.getMessage());
            unauthorized(response, "Account inactive or blocked", "ACCOUNT_INACTIVE");
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token invalid in {}: {}", request.getRequestURI(), ex.getMessage());
            unauthorized(response, "Token invalid", "TOKEN_INVALID");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message, String code)
            throws IOException{
        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"error\":\"" + message + "\",\"code\":\"" + code + "\"}");
    }
}
