package com.barbearia.barbearia.modules.account.repository;

import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.account.model.PlatformRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByGoogleSubject(String googleSubject);

    List<AppUser> findAllByPlatformRole(PlatformRole platformRole);

    List<AppUser> findAllByIdInAndBusinessId(Collection<Long> ids, Long businessId);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"userBusinesses", "userBusinesses.business"})
    Optional<AppUser> findWithBusinessById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") Long id);
}
