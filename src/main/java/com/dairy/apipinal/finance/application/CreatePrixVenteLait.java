package com.dairy.apipinal.finance.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePrixVenteLait(
        BigDecimal prixParLitre,
        LocalDate dateDebut,
        LocalDate dateFin
) {}