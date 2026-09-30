package com.dairy.apipinal.reproduction.api;

import java.time.LocalDate;
import java.util.UUID;

public record AvortementDeclareEvent(
        UUID cycleId,
        UUID animalId,
        UUID tenantId,
        UUID exploitationId,
        LocalDate dateAvortement
) {
}
