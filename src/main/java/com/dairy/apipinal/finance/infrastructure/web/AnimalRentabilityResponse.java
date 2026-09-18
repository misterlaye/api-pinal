package com.dairy.apipinal.finance.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Rentabilité économique calculée pour un animal.")
public record AnimalRentabilityResponse(

        @Schema(
                description = "Identifiant de l'animal",
                example = "7f84c2ea-7cf0-4fd5-85ad-3dbcf1f1d211"
        )
        UUID animalId,

        @Schema(
                description = "Début de la période",
                example = "2026-09-01"
        )
        LocalDate dateDebut,

        @Schema(
                description = "Fin de la période",
                example = "2026-09-30"
        )
        LocalDate dateFin,

        @Schema(
                description = "Volume de lait produit en kilogrammes",
                example = "700.000"
        )
        BigDecimal volumeLaitKg,

        @Schema(
                description = "Volume converti en litres selon la politique Finance configurée",
                example = "700.000"
        )
        BigDecimal volumeLaitLitres,

        @Schema(
                description = "Chiffre d'affaires généré par le lait de l'animal",
                example = "420000.0000"
        )
        BigDecimal chiffreAffaires,

        @Schema(
                description = "Coût d'alimentation de l'animal",
                example = "120000.0000"
        )
        BigDecimal coutAlimentation,

        @Schema(
                description = "Marge = chiffre d'affaires - coût d'alimentation",
                example = "300000.0000"
        )
        BigDecimal marge,

        @Schema(
                description = "Prix moyen obtenu par litre",
                example = "600.0000",
                nullable = true
        )
        BigDecimal prixMoyenParLitre,

        @Schema(
                description = "Coût d'alimentation moyen par litre",
                example = "171.4286",
                nullable = true
        )
        BigDecimal coutAlimentationParLitre,

        @Schema(
                description = "Indique si la marge est strictement positive",
                example = "true"
        )
        boolean rentable
) {}