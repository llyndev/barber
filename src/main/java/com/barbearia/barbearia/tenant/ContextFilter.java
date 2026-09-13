package com.barbearia.barbearia.tenant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

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

    private final BusinessSlugResolver slugResolver;
    private final BusinessRepository businessRepository;
    private final UserBusinessRepository userBusinessRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern PUBLIC_SLUG_PATH = Pattern.compile("^/public/business/([^/]+)(/.*)?$");

    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z0-9]([a-z0-9-]{0,98}[a-z0-9])?$");

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String slug = extractSlug(request);

            if (slug == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!VALID_SLUG.matcher(slug).matches()) {
                writeError(response, HttpStatus.BAD_REQUEST, "Identificador de barbearia inválido.");
                return;
            }

            Optional<Long> businessId = slugResolver.resolve(slug);

            if (businessId.isEmpty()) {
                writeError(response, HttpStatus.NOT_FOUND, "Barbearia não encontrada.");
                return;
            }

            populateContext(businessId.get());

            filterChain.doFilter(request, response);
        } finally {
            BusinessContext.clear();
        }
    }

    /**
     * Decide entre scope anônimo e scope com role.
     */
    private void populateContext(Long businessId) {
        Long userId = currentUserId();

        if (userId == null) {
            BusinessContext.setAnonymous(businessId);
            return;
        }

        Optional<BusinessRole> role = userBusinessRepository.findRoleByUserIdAndBusinessId(userId, businessId);

        role.ifPresentOrElse(
                r -> BusinessContext.set(businessId, r),
                () -> BusinessContext.setAnonymous(businessId)
        );
    }

    /**
     * Extrai o id do usuário autenticado, ou null.
     */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.isAuthenticated()) {
            return null;
        }

        if (auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.id();
        }

        return null;
    }

    /**
     * Extrai o slug da empresa a partir do header ou, como fallback,
     * da URL da requisição. Retorna {@code null} quando o slug não é encontrado.
     */
    private String extractSlug(HttpServletRequest request) {
        String fromHeader = request.getHeader(BUSINESS_HEADER);

        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader.trim().toLowerCase();
        }

        Matcher matcher = PUBLIC_SLUG_PATH.matcher(request.getRequestURI());

        if (matcher.matches()) {
            return matcher.group(1).trim().toLowerCase();
        }

        return null;
    }

    /**
     * Escreve o erro como JSON direto na resposta.
     */
    private void writeError(HttpServletResponse response, HttpStatus status, String message)

            throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", status,
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
