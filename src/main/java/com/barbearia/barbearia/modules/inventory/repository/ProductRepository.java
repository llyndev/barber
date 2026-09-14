package com.barbearia.barbearia.modules.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.barbearia.barbearia.modules.inventory.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByBusinessIdAndActiveTrue(Long businessId);
    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);
    List<Product> findAllByIdInAndBusinessId(Collection<Long> ids, Long businessId);

    @Query("""
            select p from Product p
            where p.id in :ids
            and p.business.id = :businessId
            and p.minQuantity is not null
            and p.quantity <= p.minQuantity
            """)
    List<Product> findLowStock(@Param("ids") Collection<Long> ids, @Param("businessId") Long businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.business.id = :businessId")
    Optional<Product> findByIdAndBusinessIdForUpdate(@Param("id") Long id,@Param("businessId") Long businessId);
}
