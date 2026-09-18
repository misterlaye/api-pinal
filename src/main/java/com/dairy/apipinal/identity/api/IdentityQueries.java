package com.dairy.apipinal.identity.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityQueries {

    List<ExploitationInfo> getUserExploitations(UUID utilisateurId);

    Optional<ExploitationInfo> getActiveExploitationForUser(UUID utilisateurId, Optional<UUID> requestedExploitationId);
}
