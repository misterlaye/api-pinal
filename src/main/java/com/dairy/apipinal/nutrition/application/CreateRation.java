package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.domain.OrigineRation;

import java.time.LocalDate;
import java.util.UUID;

public record CreateRation(
        UUID animalId,
        LocalDate dateDebut,
        OrigineRation origine
) {}