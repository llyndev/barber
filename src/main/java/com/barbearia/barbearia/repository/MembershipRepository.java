package com.barbearia.barbearia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.model.Business;
import com.barbearia.barbearia.model.Membership;

public interface MembershipRepository extends JpaRepository<Membership, Long>{


    Optional<Membership> findByBusinessAndUser(Business business, AppUser user
    );

    List<Membership> findByUser(AppUser user);


}
