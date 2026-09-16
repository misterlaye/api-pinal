package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.domain.OrigineRation;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateRationRequest(

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate dateDebut,

        @NotNull(message = "L'origine de la ration est obligatoire.")
        OrigineRation origine
) {
}