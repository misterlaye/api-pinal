package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.RationCostResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RationCostResponse(
        UUID rationId,
        LocalDate dateCalcul,
        List<RationCostLineResponse> lignes,
        BigDecimal coutTotal
) {

    public static RationCostResponse from(
            RationCostResult result
    ) {
        return new RationCostResponse(
                result.rationId(),
                result.dateCalcul(),
                result.lignes()
                        .stream()
                        .map(RationCostLineResponse::from)
                        .toList(),
                result.coutTotal()
        );
    }
}