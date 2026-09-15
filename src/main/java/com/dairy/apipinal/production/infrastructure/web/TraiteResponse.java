package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.domain.Traite;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TraiteResponse(
        UUID id,
        UUID lactationId,
        OffsetDateTime dateHeure,
        String type,
        BigDecimal quantiteKg,
        OffsetDateTime dateHeureSaisie
) {

    public static TraiteResponse from(Traite traite) {
        return new TraiteResponse(
                traite.getId(),
                traite.getLactationId(),
                traite.getDateHeure(),
                traite.getType().name(),
                traite.getQuantiteKg(),
                traite.getDateHeureSaisie()
        );
    }
}