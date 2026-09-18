package com.dairy.apipinal.nutrition.application;

import java.math.BigDecimal;
import java.util.UUID;

public record AddRationLine(
        UUID rationId,
        UUID animalId,
        UUID alimentId,
        BigDecimal quantite
) {
}