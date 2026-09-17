package com.dairy.apipinal.nutrition.application;

import java.math.BigDecimal;
import java.util.UUID;

public record RationCostLine(
        UUID alimentId,
        BigDecimal quantite,
        BigDecimal prixUnitaire,
        BigDecimal cout
) {
}