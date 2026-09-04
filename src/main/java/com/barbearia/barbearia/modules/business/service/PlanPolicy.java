package com.barbearia.barbearia.modules.business.service;

import com.barbearia.barbearia.exception.PlanLimitExceededException;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.business.model.InvitationStatus;
import com.barbearia.barbearia.modules.business.model.PlanType;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.InvitationRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanPolicy {

    private final UserBusinessRepository userBusinessRepository;
    private final InvitationRepository invitationRepository;

    // Valida se o usuário pode criar mais uma barbearia
    public void ensureCanCreateBusiness(AppUser user) {

        PlanType plan = user.getPlantType();
        if (plan == null) {
            throw new PlanLimitExceededException("NO_ACTIVE_PLAN");
        }

        if (!user.hasActivePlan()) {
            throw new PlanLimitExceededException("PLAN_EXPIRED");
        }

        long current = userBusinessRepository.countActiveBusinessesByUserIdAndRole(user.getId(), BusinessRole.OWNER);

        if (!user.getPlantType().allowsAnotherBusiness(current)) {
            throw new PlanLimitExceededException("BUSINESS_LIMIT_REACHED");
        }
    }

    // Valida se a barbearia pode receber mais de um barbeiro
    public void ensureCanAddBarber(Business business) {

        AppUser owner = business.getOwner();
        PlanType plan = owner.getPlantType();

        if (plan == null || !owner.hasActivePlan()) {
            throw new PlanLimitExceededException("PLAN_EXPIRED");
        }

        long active = userBusinessRepository.countByBusinessIdAndRole(business.getId(), BusinessRole.OWNER);

        long pending = invitationRepository.countPendingByBusinessIdAndRole(business.getId(), BusinessRole.BARBER, Instant.now());

        if (!plan.allowsAnotherBarber(active + pending)) {
            throw new PlanLimitExceededException("BARBER_LIMIT_REACHED");
        }
    }

}
