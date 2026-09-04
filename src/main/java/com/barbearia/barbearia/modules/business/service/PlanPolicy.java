package com.barbearia.barbearia.modules.business.service;

import com.barbearia.barbearia.exception.PlanLimitExceededException;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.InvitationStatus;
import com.barbearia.barbearia.modules.business.model.PlanType;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanPolicy {

    private final UserBusinessRepository userBusinessRepository;
    private final BusinessRepository businessRepository;

    public void ensureCanCreateBusiness(AppUser user) {
        long current = userBusinessRepository.countByUserIdAndStatus(user.getId(), user.isActive());

        if (!user.getPlantType().allowsAnotherBusiness(current)) {
            throw new PlanLimitExceededException("BUSINESS_LIMIT_REACHED");
        }
    }

    public void ensureCanAddBarber(Business business) {
        PlanType plan = business.getOwner().getPlantType();
        long current = businessRepository.countByBusinessIdAndStatusIn(business.getId(), List.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED));

        if (!plan.allowsAnotherBarber(current)) {
            throw new PlanLimitExceededException("BARBER_LIMIT_REACHED");
        }
    }

}
