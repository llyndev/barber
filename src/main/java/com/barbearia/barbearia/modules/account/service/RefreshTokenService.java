package com.barbearia.barbearia.modules.account.service;

import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.modules.account.dto.request.TokenPair;
import com.barbearia.barbearia.modules.account.dto.response.TokenResponse;
import com.barbearia.barbearia.modules.account.model.RefreshToken;
import com.barbearia.barbearia.modules.account.repository.RefreshTokenRepository;
import com.barbearia.barbearia.security.AppUserDetailsService;
import com.barbearia.barbearia.security.JwtUtil;
import com.barbearia.barbearia.security.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserDetailsService appUserDetailsService;
    private final JwtUtil jwtUtil;

    private final UserDetailsChecker checker = new AccountStatusUserDetailsChecker();

    @Transactional
    public TokenPair issue(UserDetailsImpl user, HttpServletRequest request) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Extrai o jti do token recém-gerado para persistir
        persist(jwtUtil.parseClaims(refreshToken), user.id(), request);

        return new TokenPair(accessToken, refreshToken, 900, user);
    }

    @Transactional
    public TokenPair rotate(String rawRefreshToken, HttpServletRequest request) {
        // Valida assinatura, expiração, issuer, e o type=refresh.
        Claims claims = jwtUtil.parseRefreshToken(rawRefreshToken);

        String jti = claims.getId();
        Long userId = jwtUtil.extractUserId(claims);

        RefreshToken stored = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidRequestException("Refresh token unknown"));

        if (stored.getRevokeAt() != null) {
            log.warn("Reuso de refresh token detectado! userId={} jti={}", userId, jti);
            refreshTokenRepository.revokedAllByUserId(userId, Instant.now());
            throw new SecurityException(
                    "Session invalidated for security reasons. Please log in again."
            );
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRequestException("Refresh token expired");
        }

        // Recarrega o usuario
        UserDetailsImpl user = (UserDetailsImpl) appUserDetailsService.loadUserById(userId);
        checker.check(user); // verifica se esta bloqueado, inativo ou expirado

        // Emite um novo par
        String newAccess = jwtUtil.generateAccessToken(user);
        String newRefresh = jwtUtil.generateRefreshToken(user);
        Claims newClaims = jwtUtil.parseClaims(newRefresh);

        // Revoga o antigo
        stored.setRevokeAt(Instant.now());
        stored.setReplacedByJti(newClaims.getId());

        persist(newClaims, userId, request);

        return new TokenPair(newAccess, newRefresh, 900, user);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        try {
            Claims claims = jwtUtil.parseRefreshToken(rawRefreshToken);
            refreshTokenRepository.findByJti(claims.getId())
                    .ifPresent(token -> token.setRevokeAt((Instant.now())));
        } catch (JwtException ex) {
            log.debug("Logout invalid token: {}", ex.getMessage());
        }
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        int count = refreshTokenRepository.revokedAllByUserId(userId, Instant.now());
        log.info("{} + {}", count, userId);
    }

    public void persist(Claims claims, Long userId,  HttpServletRequest request) {
        RefreshToken entity = new RefreshToken();
        entity.setJti(claims.getId());
        entity.setUserId(userId);
        entity.setExpiresAt(claims.getExpiration().toInstant());
        entity.setIpAddress(request.getRemoteAddr());

        String ua = request.getHeader("User-Agente");
        entity.setUserAgent(ua == null ? null : ua.substring(0, Math.min(ua.length(), 255)));
    }

}
