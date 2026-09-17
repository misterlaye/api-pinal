package com.dairy.apipinal.nutrition.application;

import java.time.LocalDate;
import java.util.UUID;

public record CalculateRationCost(
        UUID rationId,
        LocalDate dateCalcul
) {}