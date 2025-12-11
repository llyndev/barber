package com.barbearia.barbearia.repository;

import com.barbearia.barbearia.model.BusinessRole;
import com.barbearia.barbearia.model.UserBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBusinessRepository extends JpaRepository<UserBusiness, Long> {

    Optional<UserBusiness> findByUserIdAndBusinessId(Long userId, Long businessId);
    boolean existsByUserIdAndBusinessIdAndRole(Long userId, Long businessId, BusinessRole role);

    List<UserBusiness> findAllByBusinessIdAndRole(Long businessId, BusinessRole role);

    List<UserBusiness> findAllByBusinessId(Long businessId);

    List<UserBusiness> findAllByUserIdAndRole(Long userId, BusinessRole role);

}
