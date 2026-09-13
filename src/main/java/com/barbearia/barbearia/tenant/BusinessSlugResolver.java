package com.barbearia.barbearia.tenant;

import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolve o slug da barbearia para seu respectivo {@code businessId}.
 */
@Component
@RequiredArgsConstructor
public class BusinessSlugResolver {

    private final BusinessRepository businessRepository;
    private final Cache<String, Long> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    /**
     * Devolve o id da barbearia, ou vazio se o slug não existe.
     */
    public Optional<Long> resolve(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }

        String normalized = slug.trim().toLowerCase();

        Long cached = cache.getIfPresent(normalized);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<Long> found = businessRepository.findIdBySlug(normalized);
        found.ifPresent(id -> cache.put(normalized, id));

        return found;
    }

    /**
     * Invalida uma entrada.
     * TODO: chamar isto de onde o slug pode mudar ou a barbearia ser desativada.
     * No mínimo: BusinessService.update (quando o nome muda e o slug é
     * regerado) e BusinessService.deactivate.
     */
    public void evict(String slug) {
        if (slug != null && !slug.isBlank()) {
            cache.invalidate(slug.trim().toLowerCase());
        }
    }





}
