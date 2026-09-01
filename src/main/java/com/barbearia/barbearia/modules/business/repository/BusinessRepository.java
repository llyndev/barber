package com.barbearia.barbearia.modules.business.repository;

import java.awt.print.Pageable;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.business.model.Business;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessRepository extends JpaRepository<Business, Long>{

    @Query(value = """
            SELECT b.* FROM business b
            WHERE (:includeInactive = true OR b.active = true)
                AND (
                       unaccent(b.name)         ILIKE '%' || unaccent(:query) || '%'
                    OR unaccent(b.localidade)   ILIKE '%' || unaccent(:query) || '%'
                    OR unaccent(b.bairro)       ILIKE '%' || unaccent(:query) || '%'
                    )
            """,
            // Query para contagem
            countQuery = """
            SELECT count(*) FROM business b
            WHERE (:includeInactive = true OR b.active = true)
                AND (
                       unaccent(b.name)         ILIKE '%' || unaccent(:query) || '%'
                    OR unaccent(b.localidade)   ILIKE '%' || unaccent(:query) || '%'
                    OR unaccent(b.bairro)       ILIKE '%' || unaccent(:query) || '%'
                    )
            """,
            nativeQuery = true)
    Page<Business> search(@Param("query") String query,
                          @Param("includeInactive") boolean includeInactive,
                          Pageable pageable);

    Optional<Business> findBySlug(String slug);

    Optional<Long> findIdBySlug(String businessId);

    Optional<Business> findByOwnerId(Long id);

}
