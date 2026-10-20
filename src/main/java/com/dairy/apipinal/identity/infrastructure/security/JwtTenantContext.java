package com.dairy.apipinal.identity.infrastructure.security;

import com.dairy.apipinal.identity.api.ExploitationAccess;
import com.dairy.apipinal.identity.api.IdentityQueries;
import com.dairy.apipinal.shared.security.AuthenticationMethod;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * Résout le périmètre de la requête à partir du JWT (utilisateur, moyen d'authentification)
 * et du header {@code X-Exploitation-ID}, en vérifiant l'adhésion en base.
 * <p>
 * Une session ouverte par PIN n'accède qu'aux exploitations où l'utilisateur est ouvrier
 * (EMPLOYE, GERANT). Un refus est signalé par {@link AccessDeniedException} (HTTP 403).
 */
@Component
@Primary
public class JwtTenantContext implements TenantContext {

    private static final String EXPLOITATION_HEADER = "X-Exploitation-ID";
    private static final String RESOLVED_ATTRIBUTE_PREFIX = JwtTenantContext.class.getName() + ".resolved:";

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
    public AuthenticationMethod currentAuthenticationMethod() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof AuthenticationMethod method) {
            return method;
        }
        return AuthenticationMethod.OTP;
    }

    @Override
    public UUID currentTenantId() {
        return getExploitationAccess().tenantId();
    }

    @Override
    public UUID currentExploitationId() {
        return getExploitationAccess().exploitationId();
    }

    @Override
    public String currentRole() {
        return getExploitationAccess().role().name();
    }

    /**
     * Résolu en base au plus une fois par requête HTTP et par couple (utilisateur, exploitation
     * demandée) : un même traitement interroge le contexte des dizaines de fois. Seule une
     * résolution réussie est mémorisée ; hors requête HTTP (tâches asynchrones), aucune mémorisation.
     */
    private ExploitationAccess getExploitationAccess() {
        UUID userId = currentUserId();
        boolean workerRolesOnly = currentAuthenticationMethod() == AuthenticationMethod.PIN;
        Optional<UUID> requestedExploitationId = getRequestedExploitationIdFromHeader();

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        String cacheKey = RESOLVED_ATTRIBUTE_PREFIX + userId + ":" + workerRolesOnly + ":"
                + requestedExploitationId.map(UUID::toString).orElse("");
        if (attributes != null
                && attributes.getAttribute(cacheKey, RequestAttributes.SCOPE_REQUEST) instanceof ExploitationAccess cached) {
            return cached;
        }

        ExploitationAccess access = identityQueries
                .resolveAccess(userId, requestedExploitationId, workerRolesOnly)
                .orElseThrow(() -> new AccessDeniedException(requestedExploitationId.isPresent()
                        ? "Accès refusé à cette exploitation."
                        : "Aucune exploitation sélectionnée. Choisissez une exploitation."));

        if (attributes != null) {
            attributes.setAttribute(cacheKey, access, RequestAttributes.SCOPE_REQUEST);
        }
        return access;
    }

    private Optional<UUID> getRequestedExploitationIdFromHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
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
