package com.dairy.apipinal.health.api.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HealthEventRecorded(
        UUID eventId,
        UUID tenantId,
        UUID exploitationId,
        UUID animalId,
        OffsetDateTime dateHeure
) {
}