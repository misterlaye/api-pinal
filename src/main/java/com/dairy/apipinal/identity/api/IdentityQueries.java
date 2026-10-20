package com.dairy.apipinal.identity.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityQueries {

    List<ExploitationInfo> getUserExploitations(UUID utilisateurId);

    Optional<ExploitationInfo> getActiveExploitationForUser(UUID utilisateurId, Optional<UUID> requestedExploitationId);

    /**
     * Résout l'accès d'un utilisateur à une exploitation active.
     * <ul>
     *     <li>exploitation demandée : l'utilisateur doit en être membre ;</li>
     *     <li>aucune exploitation demandée : accepté uniquement si une seule adhésion est éligible ;</li>
     *     <li>{@code workerRolesOnly} : seules les adhésions EMPLOYE et GERANT sont éligibles (session PIN).</li>
     * </ul>
     * Aucune requête de comptage : cette méthode est appelée à chaque requête HTTP.
     */
    Optional<ExploitationAccess> resolveAccess(
            UUID utilisateurId,
            Optional<UUID> requestedExploitationId,
            boolean workerRolesOnly
    );
}
