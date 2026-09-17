package com.dairy.apipinal.finance.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RentabiliteResponse(
        UUID id,
        LocalDate periodeDebut,
        LocalDate periodeFin,
        OffsetDateTime dateCalcul,
        BigDecimal volumeLait,
        BigDecimal chiffreAffaires,
        BigDecimal coutAlimentation,
        BigDecimal autresCharges,
        BigDecimal coutTotal,
        BigDecimal coutRevientParLitre,
        BigDecimal marge
) {}