package com.dairy.apipinal.production.api.event;

import java.time.LocalDate;
import java.util.UUID;

public record LactationStarted(
        UUID lactationId,
        UUID animalId,
        UUID tenantId,
        LocalDate dateDebut
) {}