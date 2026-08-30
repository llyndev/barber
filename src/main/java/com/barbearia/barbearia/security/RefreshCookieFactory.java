package com.barbearia.barbearia.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieFactory {

    private final String name;
    private final boolean secure;
    private final String sameSite;
    private final String path;
    private final Duration maxAge;

    public RefreshCookieFactory(
            @Value("${app.auth.cookie.name:refresh_token}") String name,
            @Value("${app.auth.cookie.secure:true}") boolean secure,
            @Value("${app.auth.cookie.same-site:Strict}") String sameSite,
            @Value("${app.auth.cookie.path:/auth}") String path,
            @Value("${jwt.refresh-ttl:P7D}") Duration maxAge
    ) {
        this.name = name;
        this.secure = secure;
        this.sameSite = sameSite;
        this.path = path;
        this.maxAge = maxAge;
    }

    public String getName() {
        return name;
    }

    // Cookie com token. MaxAge igual ao TTL do JWT os dois expiram juntos
    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(name, refreshToken)
                .httpOnly(true)    // invisível para JavaScript — barreira contra XSS
                .secure(secure)    // só HTTPS
                .sameSite(sameSite)
                .path(path)        // não é enviado nas rotas normais da API
                .maxAge(maxAge)
                .build();
    }

    // Cookie de remoção. O navegador so apaga se name, path e domain baterem
    public ResponseCookie clear() {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build();
    }
}
