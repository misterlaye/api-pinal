package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.domain.TypeTraite;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecordMilkingRequest(

        @NotNull
        OffsetDateTime dateHeure,

        @NotNull
        TypeTraite type,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal quantiteKg
) {}