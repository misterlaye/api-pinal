package com.dairy.apipinal.health.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HealthEventReference(
        UUID id,
        UUID animalId,
        OffsetDateTime dateHeure,
        String description,
        String diagnostic,
        String traitement,
        LocalDate dateFin
) {}