package com.dairy.apipinal.production.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProductionQueries {

    ProductionReference getActiveLactation(UUID animalId);

    BigDecimal getMilkProductionKg(
            UUID animalId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFinExclusive
    );

    BigDecimal getTotalMilkProductionKg(
            OffsetDateTime dateDebut,
            OffsetDateTime dateFinExclusive
    );
}