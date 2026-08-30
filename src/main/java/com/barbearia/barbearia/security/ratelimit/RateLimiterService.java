package com.barbearia.barbearia.security.ratelimit;

import com.barbearia.barbearia.exception.RateLimitExceededException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;

/*
 * Guarda um bucket por chave (IP ou email) em memória.
 *
 * Caffeine com expireAfterAccess evita vazamento de memória: chaves paradas
 * somem sozinhas. Em produção com mais de uma instância isso precisa virar
 * Redis, senão cada instância conta separadamente e o limite efetivo multiplica.
 */
@Component
public class RateLimiterService {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(50_000) // teto de memória
            .build();

    // 5 tentantivas por 15 min
    private static final Bandwidth LOGIN_LIMIT = Bandwidth.builder()
            .capacity(5)
            .refillIntervally(5, Duration.ofMinutes(15))
            .build();

    // 3 registro por hora
    private static final Bandwidth REGISTER_LIMIT = Bandwidth.builder()
            .capacity(3)
            .refillIntervally(3, Duration.ofHours(1))
            .build();

    public void consume(String key, LimitType type) {

        Bucket bucket = buckets.get(type.name() + ":" + key,
                k -> Bucket.builder().addLimit(type.bandwidth()).build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long secondsToWait = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            throw new RateLimitExceededException(secondsToWait);
        }

    }

    public enum LimitType {
        LOGIN(LOGIN_LIMIT),
        REGISTER(REGISTER_LIMIT);

        private final Bandwidth bandwidth;
        LimitType(Bandwidth bandwidth) { this.bandwidth = bandwidth; }
        Bandwidth bandwidth() {return bandwidth;
        }
    }
}
