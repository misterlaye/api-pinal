package com.dairy.apipinal.finance.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Snapshot de rentabilité d'une exploitation.")
public record RentabiliteResponse(

        @Schema(description = "Identifiant du calcul")
        UUID id,

        @Schema(
                description = "Début de la période calculée",
                example = "2026-09-01"
        )
        LocalDate periodeDebut,

        @Schema(
                description = "Fin de la période calculée",
                example = "2026-09-30"
        )
        LocalDate periodeFin,

        @Schema(
                description = "Date et heure du calcul",
                example = "2026-09-30T18:00:00Z"
        )
        OffsetDateTime dateCalcul,

        @Schema(
                description = "Volume de lait utilisé pour le calcul",
                example = "1200.0000"
        )
        BigDecimal volumeLait,

        @Schema(
                description = "Chiffre d'affaires lait",
                example = "800000.0000"
        )
        BigDecimal chiffreAffaires,

        @Schema(
                description = "Coût total d'alimentation",
                example = "250000.0000"
        )
        BigDecimal coutAlimentation,

        @Schema(
                description = "Autres charges d'exploitation",
                example = "150000.0000"
        )
        BigDecimal autresCharges,

        @Schema(
                description = "Coût total",
                example = "400000.0000"
        )
        BigDecimal coutTotal,

        @Schema(
                description = "Coût de revient par litre",
                example = "333.3333"
        )
        BigDecimal coutRevientParLitre,

        @Schema(
                description = "Marge = chiffre d'affaires - coût total",
                example = "400000.0000"
        )
        BigDecimal marge
) {}