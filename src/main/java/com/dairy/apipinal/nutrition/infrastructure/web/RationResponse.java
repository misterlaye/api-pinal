package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;

import java.time.LocalDate;
import java.util.UUID;

public record RationResponse(
        UUID id,
        UUID tenantId,
        UUID animalId,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutRation statut,
        OrigineRation origine
) {

    public static RationResponse from(Ration ration) {
        return new RationResponse(
                ration.getId(),
                ration.getTenantId(),
                ration.getAnimalId(),
                ration.getDateDebut(),
                ration.getDateFin(),
                ration.getStatut(),
                ration.getOrigine()
        );
    }
}