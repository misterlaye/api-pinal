package com.dairy.apipinal.nutrition.infrastructure.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddRationLineRequest(

        @NotNull(message = "L'aliment est obligatoire.")
        UUID alimentId,

        @NotNull(message = "La quantité est obligatoire.")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "La quantité doit être strictement positive."
        )
        BigDecimal quantite
) {}