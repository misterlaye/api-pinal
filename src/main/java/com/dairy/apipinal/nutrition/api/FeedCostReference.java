package com.dairy.apipinal.nutrition.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FeedCostReference(
        UUID animalId,
        LocalDate dateDebut,
        LocalDate dateFinExclusive,
        BigDecimal coutAlimentation
) {}