package com.barbearia.barbearia.tenant;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.UserBusiness;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContextFilter extends OncePerRequestFilter{

    private final BusinessRepository businessRepository;
    private final UserBusinessRepository userBusinessRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String businessSlug = request.getHeader("X-Business-Slug");

        if (businessSlug == null || businessSlug.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Business business = businessRepository.findBySlug(businessSlug)
                    .orElse(null);

            if (business == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Business not found");
                return;
            }

            var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                Long userId = userDetails.user().getId();

                UserBusiness membership = userBusinessRepository
                        .findByUserIdAndBusinessId(userId, business.getId())
                        .orElse(null);

                if (membership == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a member of this business");
                }

                BusinessContext.setBusinessId(business.getId().toString());
                BusinessContext.setBusinessRole(membership.getRole().name());
            } else {
                BusinessContext.setBusinessId(business.getId().toString());
            }

            filterChain.doFilter(request, response);

        } finally {
            BusinessContext.clear();
        }
    }
}
