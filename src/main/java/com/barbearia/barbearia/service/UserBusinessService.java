package com.barbearia.barbearia.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barbearia.barbearia.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.UserBusinessMapper;
import com.barbearia.barbearia.model.BusinessRole;
import com.barbearia.barbearia.model.UserBusiness;
import com.barbearia.barbearia.repository.UserBusinessRepository;
import com.barbearia.barbearia.tenant.BusinessContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserBusinessService {

    private final UserBusinessRepository userBusinessRepository;
    private final UserBusinessMapper userBusinessMapper;
    

    private Long getBusinessIdFromContext() {
        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null || businessIdStr.isBlank()) {
            throw new IllegalStateException("Business ID não encontrado no contexto.");
        }
        return Long.parseLong(businessIdStr);
    }

    private void checkOwnerManagerPermission() {
        String role = BusinessContext.getBusinessRole();
        if (!"OWNER".equals(role) && !"MANAGER".equals(role)) {
            throw new SecurityException("Permissão negada. Requer ROLE de OWNER ou MANAGER.");
        }
    }

    @Transactional(readOnly = true)
    public List<UserBusinessResponse> listUsersInMyBusiness() {
        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();

        List<UserBusiness> memberships = userBusinessRepository.findAllByBusinessId(businessId);

        return memberships.stream()
                .map(userBusinessMapper::toResponse)
                .toList();
    }

    @Transactional
    public void removeUserFromMyBusiness(Long userId) {
        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();

        UserBusiness link = userBusinessRepository.findByUserIdAndBusinessId(userId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (link.getRole() == BusinessRole.OWNER) {
            throw new IllegalArgumentException("It is not possible to remove the OWNER from the company.");
        }

        userBusinessRepository.delete(link);
    }

}
