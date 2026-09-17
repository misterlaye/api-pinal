package com.dairy.apipinal.nutrition.api;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface NutritionQueries {

    RationReference getRation(UUID rationId);

    boolean exists(UUID rationId);

    Optional<RationReference> findActiveRation(
            UUID animalId,
            LocalDate date
    );

    Optional<RationCostReference> calculateFeedCost(
            UUID animalId,
            LocalDate date
    );
}