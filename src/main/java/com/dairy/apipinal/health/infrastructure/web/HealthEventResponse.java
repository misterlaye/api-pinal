package com.dairy.apipinal.health.infrastructure.web;

import com.dairy.apipinal.health.domain.EvenementSanitaire;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HealthEventResponse(
        UUID id,
        UUID animalId,
        OffsetDateTime dateHeure,
        String description,
        String diagnostic,
        String traitement,
        LocalDate dateFin
) {

    public static HealthEventResponse from(
            EvenementSanitaire event
    ) {
        return new HealthEventResponse(
                event.getId(),
                event.getAnimalId(),
                event.getDateHeure(),
                event.getDescription(),
                event.getDiagnostic(),
                event.getTraitement(),
                event.getDateFin()
        );
    }
}