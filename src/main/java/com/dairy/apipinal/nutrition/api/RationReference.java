package com.dairy.apipinal.nutrition.api;

import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.StatutRation;

import java.time.LocalDate;
import java.util.UUID;

public record RationReference(
        UUID id,
        UUID tenantId,
        UUID animalId,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutRation statut,
        OrigineRation origine
) {}