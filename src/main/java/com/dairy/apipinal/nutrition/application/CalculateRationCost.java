package com.dairy.apipinal.nutrition.application;

import java.time.LocalDate;
import java.util.UUID;

public record CalculateRationCost(
        UUID animalId,
        UUID rationId,
        LocalDate dateCalcul
) {}