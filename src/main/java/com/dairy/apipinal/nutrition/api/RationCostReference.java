package com.dairy.apipinal.nutrition.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RationCostReference(
        UUID rationId,
        UUID animalId,
        LocalDate dateCalcul,
        BigDecimal coutAlimentation
) {}