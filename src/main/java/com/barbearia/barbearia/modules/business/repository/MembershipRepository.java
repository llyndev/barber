package com.barbearia.barbearia.modules.business.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.Membership;

public interface MembershipRepository extends JpaRepository<Membership, Long>{


    Optional<Membership> findByBusinessAndUser(Business business, AppUser user
    );

    List<Membership> findByUser(AppUser user);


}
