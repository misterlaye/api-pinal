package com.dairy.apipinal.nutrition.infrastructure.web;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TerminateRationRequest(
        @NotNull(message = "La date de fin est obligatoire.")
        LocalDate dateFin
) {}