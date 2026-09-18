package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.domain.LigneRation;

import java.math.BigDecimal;
import java.util.UUID;

public record RationLineResponse(
        UUID id,
        UUID alimentId,
        BigDecimal quantite
) {

    public static RationLineResponse from(
            LigneRation ligne
    ) {
        return new RationLineResponse(
                ligne.getId(),
                ligne.getAlimentId(),
                ligne.getQuantite()
        );
    }
}