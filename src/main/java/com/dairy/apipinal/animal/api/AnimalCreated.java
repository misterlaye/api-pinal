package com.dairy.apipinal.animal.api;

import java.util.UUID;

public record AnimalCreated(
        UUID animalId,
        UUID tenantId,
        UUID exploitationId,
        UUID raceId
) {}