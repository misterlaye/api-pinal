package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.RationCostLine;

import java.math.BigDecimal;
import java.util.UUID;

public record RationCostLineResponse(
        UUID alimentId,
        BigDecimal quantite,
        BigDecimal prixUnitaire,
        BigDecimal cout
) {

    public static RationCostLineResponse from(
            RationCostLine line
    ) {
        return new RationCostLineResponse(
                line.alimentId(),
                line.quantite(),
                line.prixUnitaire(),
                line.cout()
        );
    }
}