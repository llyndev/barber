package com.barbearia.barbearia.modules.business.repository;

import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.business.model.UserBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBusinessRepository extends JpaRepository<UserBusiness, Long> {

    Optional<UserBusiness> findByUserIdAndBusinessId(Long userId, Long businessId);
    boolean existsByUserIdAndBusinessIdAndRole(Long userId, Long businessId, BusinessRole role);

    List<UserBusiness> findAllByBusinessIdAndRole(Long businessId, BusinessRole role);

    List<UserBusiness> findAllByBusinessId(Long businessId);

    List<UserBusiness> findAllByUserIdAndRole(Long userId, BusinessRole role);

    long countByUserIdAndRole(Long userId, BusinessRole role);

    long countByBusinessIdAndRole(Long businessId, BusinessRole role);

    boolean existsByUserIdAndBusinessIdAndRoleIn(Long userId, Long businessId, List<BusinessRole> roles);

}
