package com.dairy.apipinal.nutrition.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePrixAliment(
        UUID alimentId,
        BigDecimal prixUnitaire,
        LocalDate dateDebut,
        LocalDate dateFin
) {}