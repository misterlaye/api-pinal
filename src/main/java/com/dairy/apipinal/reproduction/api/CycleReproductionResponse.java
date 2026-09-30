package com.dairy.apipinal.reproduction.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CycleReproductionResponse(
        UUID id,
        UUID animalId,
        int numeroCycle,
        String statut,
        LocalDate dateInsemination,
        String methodeReproduction,
        UUID taureauId,
        String codePaillette,
        LocalDate datePrevueVelage,
        LocalDate dateReelleVelage,
        List<ConstatGestationResponse> constats,
        OffsetDateTime createdAt
) {
    public record ConstatGestationResponse(
            LocalDate date,
            String resultat,
            String veterinaire
    ) {}
}
