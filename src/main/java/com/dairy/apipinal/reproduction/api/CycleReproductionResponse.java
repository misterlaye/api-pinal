package com.dairy.apipinal.reproduction.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CycleReproductionResponse(
        UUID id,
        UUID animalId,
        int numeroCycle,
        String statut,
        LocalDate dateInsemination,
        String methodeReproduction,
        String identifiantTaureau,
        LocalDate datePrevueVelage,
        LocalDate dateReelleVelage,
        LocalDate constatDate,
        String constatResultat,
        String constatVeterinaire,
        OffsetDateTime createdAt
) {
}
