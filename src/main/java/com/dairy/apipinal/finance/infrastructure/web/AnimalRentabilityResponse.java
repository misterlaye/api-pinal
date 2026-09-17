package com.dairy.apipinal.finance.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnimalRentabilityResponse(
        UUID animalId,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal volumeLaitKg,
        BigDecimal volumeLaitLitres,
        BigDecimal chiffreAffaires,
        BigDecimal coutAlimentation,
        BigDecimal marge,
        BigDecimal prixMoyenParLitre,
        BigDecimal coutAlimentationParLitre,
        boolean rentable
) {}