package com.dairy.apipinal.finance.application;

import java.time.LocalDate;

public record CalculateRentabilite(
        LocalDate periodeDebut,
        LocalDate periodeFin
) {}