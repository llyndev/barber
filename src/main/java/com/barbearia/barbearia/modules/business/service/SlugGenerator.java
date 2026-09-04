package com.barbearia.barbearia.modules.business.service;

import com.barbearia.barbearia.common.util.TextNormalizer;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SlugGenerator {

    private final BusinessRepository businessRepository;

    // Gera um slug unico
    public String generateUnique(String slug) {
        String base = TextNormalizer.toSlug(slug);

        if (RESERVED_SLUGS.contains(base)) {
            base = base + "-barbearia";
        }

        String candidate = base;
        int suffix = 2;

        // "barber-cuttz, depois "barber-cuttz-1, "-3"...
        while (businessRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;

            if (suffix > 100) {
                throw new IllegalStateException("Could not generate a unique slug for: " + slug);
            }
        }
        return candidate;

    }

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "auth", "login", "register", "admin", "api", "cep",
            "business", "health", "actuator", "www", "app"
    );

}
