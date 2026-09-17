package com.dairy.apipinal.finance.infrastructure.web;

import com.dairy.apipinal.finance.api.FinanceQueries;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

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

    @GetMapping("/rentabilite/latest")
    public ResponseEntity<RentabiliteResponse> getLatestRentabilite() {

        return financeQueries
                .findLatestRentabilite()
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/animals/{animalId}/rentabilite")
    public ResponseEntity<AnimalRentabilityResponse> getAnimalRentability(
            @PathVariable UUID animalId,
            @RequestParam @NotNull LocalDate dateDebut,
            @RequestParam @NotNull LocalDate dateFin
    ) {

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