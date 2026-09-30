package com.dairy.apipinal.production.api.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MilkingRecorded(
        UUID traiteId,
        UUID lactationId,
        UUID animalId,
        UUID tenantId,
        UUID exploitationId,
        BigDecimal quantiteKg,
        OffsetDateTime dateHeure
) {}