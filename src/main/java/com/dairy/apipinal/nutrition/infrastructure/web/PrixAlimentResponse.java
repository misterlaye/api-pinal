package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.domain.PrixAliment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrixAlimentResponse(
        UUID id,
        UUID alimentId,
        BigDecimal prixUnitaire,
        LocalDate dateDebut,
        LocalDate dateFin
) {

    public static PrixAlimentResponse from(
            PrixAliment prixAliment
    ) {
        return new PrixAlimentResponse(
                prixAliment.getId(),
                prixAliment.getAlimentId(),
                prixAliment.getPrixUnitaire(),
                prixAliment.getDateDebut(),
                prixAliment.getDateFin()
        );
    }
}