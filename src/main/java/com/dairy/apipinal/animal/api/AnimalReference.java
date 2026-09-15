package com.dairy.apipinal.animal.api;

import java.util.UUID;

public record AnimalReference(
        UUID id,
        UUID tenantId,
        UUID exploitationId,
        UUID raceId,
        String identifiant,
        String nom,
        String statut
) {}
