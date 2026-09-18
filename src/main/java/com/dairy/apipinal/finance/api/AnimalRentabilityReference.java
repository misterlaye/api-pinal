package com.dairy.apipinal.finance.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnimalRentabilityReference(
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