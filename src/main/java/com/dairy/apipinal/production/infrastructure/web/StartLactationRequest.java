package com.dairy.apipinal.production.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record StartLactationRequest(

        @NotNull
        UUID animalId,

        @NotNull
        LocalDate dateDebut
) {}