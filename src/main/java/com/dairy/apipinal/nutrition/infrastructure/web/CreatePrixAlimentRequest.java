package com.dairy.apipinal.nutrition.infrastructure.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePrixAlimentRequest(

        @NotNull(message = "Le prix unitaire est obligatoire.")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "Le prix unitaire doit être strictement positif."
        )
        BigDecimal prixUnitaire,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate dateDebut,

        LocalDate dateFin
) {}