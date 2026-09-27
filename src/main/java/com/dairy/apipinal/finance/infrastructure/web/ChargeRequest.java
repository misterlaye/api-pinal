package com.dairy.apipinal.finance.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ChargeRequest(
        @NotBlank String libelle,
        @NotBlank String categorie,
        @NotNull @PositiveOrZero BigDecimal montant,
        @NotNull LocalDate date,
        String description
) {}
