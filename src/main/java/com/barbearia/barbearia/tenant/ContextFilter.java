package com.barbearia.barbearia.tenant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.barbearia.barbearia.modules.business.model.UserBusiness;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Slf4j
@RequiredArgsConstructor
public class ContextFilter extends OncePerRequestFilter {

    private static final String BUSINESS_HEADER = "X-Business-Slug";

    private final BusinessRepository businessRepository;
    private final UserBusinessRepository userBusinessRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuador");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String businessSlug = request.getHeader(BUSINESS_HEADER);

        if (businessSlug == null || businessSlug.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Normaliza para o slug ser case-insensitive e tolerante a espaços
        String slug = businessSlug.trim().toLowerCase();

        try {
            Long businessId = businessRepository.findIdBySlug(businessSlug)
                    .orElse(null);

            if (businessId == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "Business not found.");
                return;
            }

            var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                Long userId = userDetails.user().getId();

                UserBusiness membership = userBusinessRepository
                        .findByUserIdAndBusinessId(userId, businessId)
                        .orElse(null);

                if (membership == null) {
                    log.warn("Access denied: user{} tried access business{}", userId, slug);
                    writeError(response, HttpServletResponse.SC_FORBIDDEN,
                            "User is not a member of this business.");
                    return;
                }

                BusinessContext.set(businessId, membership.getRole());
            }

            filterChain.doFilter(request, response);

        } finally {
            BusinessContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, int status, String message)
        throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                Map.of("status", status, "message", message));
    }
}
