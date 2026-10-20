package com.dairy.apipinal.production.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProductionQueries {

    ProductionReference getActiveLactation(UUID animalId);

    BigDecimal getMilkProductionLitres(
            UUID animalId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFinExclusive
    );

    /**
     * Volume de lait (litres) par jour civil UTC de l'exploitation courante, sur [debut, finInclusive].
     * Les jours sans traite sont absents de la carte (aucun volume n'est inventé).
     */
    java.util.Map<java.time.LocalDate, BigDecimal> getDailyMilkProductionLitres(
            java.time.LocalDate debut,
            java.time.LocalDate finInclusive
    );

    BigDecimal getTotalMilkProductionLitres(
            OffsetDateTime dateDebut,
            OffsetDateTime dateFinExclusive
    );
}