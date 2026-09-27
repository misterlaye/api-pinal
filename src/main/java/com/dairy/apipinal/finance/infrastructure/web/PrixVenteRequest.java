package com.dairy.apipinal.finance.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PrixVenteRequest(
        @NotNull @Positive BigDecimal prixParLitre,
        @NotNull LocalDate dateDebut,
        LocalDate dateFin
) {}
