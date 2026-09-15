package com.dairy.apipinal.production.api;

import com.dairy.apipinal.production.domain.StatutLactation;

import java.time.LocalDate;
import java.util.UUID;

public record ProductionReference(
        UUID lactationId,
        UUID animalId,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutLactation statut
) {}