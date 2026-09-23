package com.dairy.apipinal.identity.infrastructure.security;

import com.dairy.apipinal.identity.api.ExploitationInfo;
import com.dairy.apipinal.identity.api.IdentityQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Component
@Primary
@Profile("!dev")
public class JwtTenantContext implements TenantContext {

    private static final String EXPLOITATION_HEADER = "X-Exploitation-ID";

    private final IdentityQueries identityQueries;

    public JwtTenantContext(IdentityQueries identityQueries) {
        this.identityQueries = identityQueries;
    }

    @Override
    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        throw new IllegalStateException("Aucun utilisateur authentifié ou jeton JWT manquant.");
    }

    @Override
    public UUID currentTenantId() {
        UUID userId = currentUserId();
        Optional<UUID> requestedExploitationId = getRequestedExploitationIdFromHeader();

        return identityQueries.getActiveExploitationForUser(userId, requestedExploitationId)
                .map(ExploitationInfo::tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "L'utilisateur " + userId + " n'a accès à aucune exploitation active."
                ));
    }

    private Optional<UUID> getRequestedExploitationIdFromHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        HttpServletRequest request = attributes.getRequest();
        String headerValue = request.getHeader(EXPLOITATION_HEADER);
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                return Optional.of(UUID.fromString(headerValue.trim()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Header " + EXPLOITATION_HEADER + " invalide : " + headerValue);
            }
        }
        return Optional.empty();
    }
}

