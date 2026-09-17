package com.dairy.apipinal.nutrition.application;

import java.time.LocalDate;
import java.util.UUID;

public record GetApplicablePrixAliment(
        UUID alimentId,
        LocalDate date
) {}