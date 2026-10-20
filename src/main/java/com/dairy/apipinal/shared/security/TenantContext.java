package com.dairy.apipinal.shared.security;

import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

/**
 * Périmètre de la requête courante : utilisateur, exploitation active, tenant et rôle.
 * Toute lecture ou écriture métier doit être limitée à {@link #currentExploitationId()}.
 */
public interface TenantContext {

    UUID currentTenantId();

    UUID currentExploitationId();

    UUID currentUserId();

    String currentRole();

    default AuthenticationMethod currentAuthenticationMethod() {
        return AuthenticationMethod.OTP;
    }

    /**
     * Retourne l'exploitation active, en vérifiant qu'une exploitation éventuellement transmise
     * par le client (paramètre, corps de requête) est bien celle du contexte.
     *
     * @throws AccessDeniedException si l'exploitation demandée diffère de l'exploitation active.
     */
    default UUID requireExploitation(UUID requestedExploitationId) {
        UUID current = currentExploitationId();
        if (requestedExploitationId != null && !requestedExploitationId.equals(current)) {
            throw new AccessDeniedException("Accès refusé à cette exploitation.");
        }
        return current;
    }
}
