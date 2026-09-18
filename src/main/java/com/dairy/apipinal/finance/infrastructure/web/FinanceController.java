package com.dairy.apipinal.finance.infrastructure.web;

import com.dairy.apipinal.finance.api.FinanceQueries;
import com.dairy.apipinal.finance.application.InvalidRentabilityPeriodException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Tag(
        name = "Finance",
        description = "Consultation des indicateurs de rentabilité de l'exploitation et des animaux."
)
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceQueries financeQueries;
    private final FinanceRestMapper mapper;

    public FinanceController(
            FinanceQueries financeQueries,
            FinanceRestMapper mapper
    ) {
        this.financeQueries = financeQueries;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Consulter la dernière rentabilité de l'exploitation",
            description = """
                Retourne le dernier calcul de rentabilité disponible
                pour l'exploitation du tenant courant.

                Le résultat est un snapshot précédemment calculé et persisté.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Calcul de rentabilité trouvé",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = RentabiliteResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Rentabilité exploitation",
                                    value = """
                                        {
                                          "id": "5f6e0f8b-4f0d-4c87-8bb7-4bdc7a4a6f31",
                                          "periodeDebut": "2026-09-01",
                                          "periodeFin": "2026-09-30",
                                          "dateCalcul": "2026-09-30T18:00:00Z",
                                          "volumeLait": 1200.0000,
                                          "chiffreAffaires": 800000.0000,
                                          "coutAlimentation": 250000.0000,
                                          "autresCharges": 150000.0000,
                                          "coutTotal": 400000.0000,
                                          "coutRevientParLitre": 333.3333,
                                          "marge": 400000.0000
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentification requise"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Aucun calcul de rentabilité disponible"
            )
    })
    @GetMapping("/rentabilite/latest")
    public ResponseEntity<RentabiliteResponse> getLatestRentabilite() {

        return financeQueries
                .findLatestRentabilite()
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void validatePeriod(
            UUID animalId,
            LocalDate periodeDebut,
            LocalDate periodeFin
    ) {
        if (animalId == null) {
            throw new InvalidRentabilityPeriodException(
                    "L'identifiant de l'animal est obligatoire."
            );
        }

        if (periodeDebut == null || periodeFin == null) {
            throw new InvalidRentabilityPeriodException(
                    "La période est obligatoire."
            );
        }

        if (periodeFin.isBefore(periodeDebut)) {
            throw new InvalidRentabilityPeriodException(
                    "La date de fin doit être supérieure ou égale à la date de début."
            );
        }
    }

    @Operation(
            summary = "Calculer la rentabilité d'un animal",
            description = """
                Calcule la rentabilité économique d'un animal sur une période donnée.

                Le chiffre d'affaires est obtenu à partir de la production
                laitière convertie en litres et des prix de vente historiques
                applicables.

                Le coût pris en compte au niveau animal est le coût
                d'alimentation. Les autres charges communes de l'exploitation
                ne sont pas imputées à l'animal.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Rentabilité calculée",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AnimalRentabilityResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Rentabilité animal",
                                    value = """
                                        {
                                          "animalId": "7f84c2ea-7cf0-4fd5-85ad-3dbcf1f1d211",
                                          "dateDebut": "2026-09-01",
                                          "dateFin": "2026-09-30",
                                          "volumeLaitKg": 700.000,
                                          "volumeLaitLitres": 700.000,
                                          "chiffreAffaires": 420000.0000,
                                          "coutAlimentation": 120000.0000,
                                          "marge": 300000.0000,
                                          "prixMoyenParLitre": 600.0000,
                                          "coutAlimentationParLitre": 171.4286,
                                          "rentable": true
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Période invalide"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentification requise"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                        Rentabilité impossible à calculer pour cette période,
                        par exemple lorsqu'aucun coût d'alimentation exploitable
                        n'est disponible.
                        """
            )
    })
    @GetMapping("/animals/{animalId}/rentabilite")
    public ResponseEntity<AnimalRentabilityResponse> getAnimalRentability(
            @Parameter(
                    description = "Identifiant UUID de l'animal",
                    required = true,
                    example = "7f84c2ea-7cf0-4fd5-85ad-3dbcf1f1d211"
            )
            @PathVariable UUID animalId,

            @Parameter(
                    description = "Date de début incluse",
                    required = true,
                    example = "2026-09-01"
            )
            @RequestParam @NotNull LocalDate dateDebut,

            @Parameter(
                    description = "Date de fin incluse",
                    required = true,
                    example = "2026-09-30"
            )
            @RequestParam @NotNull LocalDate dateFin
    ) {

        validatePeriod(animalId, dateDebut, dateFin);

        return financeQueries
                .calculateAnimalRentability(
                        animalId,
                        dateDebut,
                        dateFin
                )
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}