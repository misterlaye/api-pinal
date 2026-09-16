package com.dairy.apipinal.nutrition.application;

import java.time.LocalDate;
import java.util.UUID;

public record TerminateRation(
        UUID animalId,
        UUID rationId,
        LocalDate dateFin
) {}