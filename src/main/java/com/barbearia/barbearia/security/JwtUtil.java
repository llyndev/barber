package com.barbearia.barbearia.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final String issuer;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtUtil(@Value("${jwt.secret}") String secretBase64,
                   @Value("${jwt.issuer:barbercuttz}") String issuer,
                   @Value("${jwt.access-ttl:PT15M}") Duration accessTtl,   // formato ISO-8601
                   @Value("${jwt.refresh-ttl:P7D}") Duration refreshTtl) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "jwt.secret must be Base64-encoded. Generate it using: openssl rand -base64 48", ex
            );
        }

        // hmacShaKeyFor já rejeita chave com menos de 256 bits (WeakKeyException),
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 bytes (256 bits) for HS256"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String generateAccessToken(UserDetailsImpl user) {
        // Role vai na claim para o filtro não precisar consultar o banco em
        // requisições onde só a permissão importa
        return buildToken(user, TYPE_ACCESS, accessTtl,
                Map.of(CLAIM_EMAIL, user.email(),
                        CLAIM_ROLE, user.platformRole().name()));
    }

    public String generateRefreshToken(UserDetailsImpl user) {
        // Refresh não carrega role: se a permissão mudar, o novo access token
        // já sai com o valor atualizado
        return buildToken(user, TYPE_REFRESH, refreshTtl, Map.of());
    }

    private String buildToken(UserDetailsImpl user, String type,
                              Duration ttl, Map<String, Object> extraClaims) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .issuer(issuer)              // impede token de dev valer em produção
                .id(UUID.randomUUID().toString())  // jti: permite revogar token específico
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claims(extraClaims)
                .claim(CLAIM_TYPE, type)     // separa access de refresh
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }


    /**
     * Parse único que valida assinatura, expiração e issuer de uma vez.
     * Deixa as exceções subirem de propósito: quem chama decide o que fazer
     * com ExpiredJwtException (renovar) e SignatureException (alertar).
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)   // restringe a algoritmos HMAC: bloqueia alg=none
                .requireIssuer(issuer)   // rejeita token de outro ambiente
                .clockSkewSeconds(60)    // tolera dessincronia de relógio entre servidores
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Valida que é um access token
    public Claims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("A refresh token cannot be used as an access token.");
        }
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if(!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("The provided token is not a refresh token.");
        }
        return claims;
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

}
