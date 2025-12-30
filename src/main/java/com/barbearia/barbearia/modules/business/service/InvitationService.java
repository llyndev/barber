package com.barbearia.barbearia.modules.business.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.barbearia.barbearia.modules.business.dto.request.AddUserToBusinessRequest;
import com.barbearia.barbearia.modules.business.dto.response.InvitationResponse;
import com.barbearia.barbearia.modules.business.dto.response.UserBusinessResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.business.mapper.InvitationMapper;
import com.barbearia.barbearia.modules.business.mapper.UserBusinessMapper;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.business.model.Invitation;
import com.barbearia.barbearia.modules.business.model.PlanType;
import com.barbearia.barbearia.modules.business.model.UserBusiness;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.InvitationRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.tenant.BusinessContext;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final UserRepository userRepository;
    private final UserBusinessRepository userBusinessRepository;
    private final InvitationRepository invitationRepository;
    private final BusinessRepository businessRepository;
    private final UserBusinessMapper userBusinessMapper;
    private final InvitationMapper invitationMapper;

    @Transactional
    public InvitationResponse createInvitation(AddUserToBusinessRequest request) {
        checkOwnerManagerPermission();
        Long businessId = getBusinessIdFromContext();

        AppUser userToInvite = userRepository.findByEmail(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User email not found"));

        userBusinessRepository.findByUserIdAndBusinessId(userToInvite.getId(), businessId).ifPresent(
            link -> {
                throw new IllegalArgumentException("The user is already a member of this company");
            }
        );

        invitationRepository.findByBusinessIdAndEmailAndStatus(businessId, request.userEmail(), Invitation.Status.PENDING).ifPresent(inv -> {
            throw new IllegalArgumentException("There is already a pending invitation for this user.");
        });

        if (request.role() == BusinessRole.OWNER) {
            throw new IllegalArgumentException("Cannot invite a user to be OWNER.");
        }

        if (request.role() == BusinessRole.MANAGER) {
             String currentRole = BusinessContext.getBusinessRole();
             if (!"OWNER".equals(currentRole)) {
                 throw new SecurityException("Only the OWNER can invite a MANAGER.");
             }
        }

        if (request.role() == BusinessRole.BARBER) {
            UserBusiness ownerLink = userBusinessRepository.findAllByBusinessIdAndRole(businessId, BusinessRole.OWNER)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Barber shops don't have owners."));

            AppUser owner = ownerLink.getUser();

            PlanType ownerPlan;
            try {
                ownerPlan = PlanType.valueOf(owner.getPlantType());
            } catch (Exception e) {
                throw new IllegalStateException("Barber shop owner without a plan set up.");
            }

            long currentBarbers = userBusinessRepository.countByBusinessIdAndRole(businessId, BusinessRole.BARBER);

            long pendingInvites = invitationRepository.findAll().stream()
                .filter(i -> i.getBusiness().getId().equals(businessId)
                        && i.getRole() == BusinessRole.BARBER 
                        && i.getStatus() == Invitation.Status.PENDING)
                .count();

            if ((currentBarbers + pendingInvites) >= ownerPlan.getMaxBarbers()) {
                throw new IllegalStateException("The plan " + ownerPlan.name() + 
                    " allows a maximum of " + ownerPlan.getMaxBarbers() + " barbers. Upgrade the plan.");
            }
        }

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Invitation invitation = Invitation.builder()
                .business(business)
                .email(request.userEmail())
                .role(request.role())
                .status(Invitation.Status.PENDING)
                .token(UUID.randomUUID().toString()) // Token único
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)) // Convite expira em 7 dias
                .build();

        invitationRepository.save(invitation);

        return invitationMapper.toResponse(invitation);
            
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyPendingInvitations(UserDetailsImpl userDetails) {
        String userEmail = userDetails.user().getEmail();
        List<Invitation> invitations = invitationRepository.findByEmailAndStatus(userEmail, Invitation.Status.PENDING);

        return invitations.stream()
                .map(invitationMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserBusinessResponse acceptInvitation(Long invitationId, UserDetailsImpl userDetails) {
        String userEmail = userDetails.user().getEmail();
        AppUser user = userDetails.user();

        Invitation invitation = invitationRepository.findByIdAndEmailAndStatus(invitationId, userEmail, Invitation.Status.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
    
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(Invitation.Status.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitaiton expired.");
        }
        
        invitation.setStatus(Invitation.Status.ACCEPTED);
        invitationRepository.save(invitation);

        UserBusiness link = UserBusiness.builder()
                .user(user)
                .business(invitation.getBusiness())
                .role(invitation.getRole())
                .build();

        userBusinessRepository.save(link);

        return userBusinessMapper.toResponse(link);
    }

    @Transactional
    public void declineInvitation(Long invitationId, UserDetailsImpl userDetails) {
        String userEmail = userDetails.user().getEmail();

        Invitation invitation = invitationRepository.findByIdAndEmailAndStatus(invitationId, userEmail, Invitation.Status.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        invitation.setStatus(Invitation.Status.CANCELED);
        invitationRepository.save(invitation);
    }

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
    
}
