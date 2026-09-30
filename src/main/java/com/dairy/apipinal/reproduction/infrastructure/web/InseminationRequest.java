package com.dairy.apipinal.reproduction.infrastructure.web;

import java.time.LocalDate;
import java.util.UUID;

public record InseminationRequest(
        UUID animalId,
        UUID exploitationId,
        LocalDate dateInsemination,
        String methodeReproduction,
        UUID taureauId,
        String codePaillette
) {
}
