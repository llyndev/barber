package com.barbearia.barbearia.modules.account.repository;

import com.barbearia.barbearia.modules.account.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    List<AppUser> findAllByPlatformRole(AppUser.PlatformRole platformRole);

    boolean existsByEmail(String email);
}
