package com.dairy.apipinal.nutrition.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RationCostResult(
        UUID rationId,
        LocalDate dateCalcul,
        List<RationCostLine> lignes,
        BigDecimal coutTotal
) {}