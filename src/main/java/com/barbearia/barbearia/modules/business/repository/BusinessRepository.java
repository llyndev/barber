package com.barbearia.barbearia.modules.business.repository;

import java.util.List;
import java.util.Optional;

import com.barbearia.barbearia.modules.business.model.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.business.model.Business;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessRepository extends JpaRepository<Business, Long>{

    @Query(value = """
            SELECT b.* FROM business b
            WHERE (:includeInactive = true OR b.active = true)
                AND (
                       unaccent(b.name)         ILIKE '%' || unaccent(:query) || '%' ESCAPE '\'
                    OR unaccent(b.localidade)   ILIKE '%' || unaccent(:query) || '%' ESCAPE '\'
                    OR unaccent(b.bairro)       ILIKE '%' || unaccent(:query) || '%' ESCAPE '\'
                    )
            """,
            // Query para contagem
            countQuery = """
            SELECT count(*) FROM business b
            WHERE (:includeInactive = true OR b.active = true)
                AND (
                       unaccent(b.name)         ILIKE '%' || unaccent(:query) || '%' ESCAPE '\'
                    OR unaccent(b.localidade)   ILIKE '%' || unaccent(:query) || '%' ESCAPE '\'
                    OR unaccent(b.bairro)       ILIKE '%' || unaccent(:query) || '%' ESCAPE '\'
                    )
            """,
            nativeQuery = true)
    Page<Business> search(@Param("query") String query,
                          @Param("includeInactive") boolean includeInactive,
                          Pageable pageable);

    Optional<Business> findBySlug(String slug);

    Optional<Long> findIdBySlug(String businessId);

    Optional<Business> findByOwnerId(Long id);

    @Query("SELECT b FROM Business b LEFT JOIN FETCH b.owner WHERE b.id = :id")
    Optional<Business> findByIdWithOwner(@Param("id") Long id);

    Page<Business> findAllByActiveTrue(Pageable pageable);

    boolean existsBySlug(String slug);

    long countByBusinessIdAndStatusIn(Long businessId, List<InvitationStatus> status);

}
