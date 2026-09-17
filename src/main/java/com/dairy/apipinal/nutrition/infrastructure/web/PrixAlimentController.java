package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.GetPrixAlimentHistory;
import com.dairy.apipinal.nutrition.application.PrixAlimentService;
import com.dairy.apipinal.nutrition.application.CreatePrixAliment;
import com.dairy.apipinal.nutrition.domain.PrixAliment;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aliments")
public class PrixAlimentController {

    private final PrixAlimentService prixAlimentService;

    public PrixAlimentController(
            PrixAlimentService prixAlimentService
    ) {
        this.prixAlimentService = prixAlimentService;
    }

    @PostMapping("/{alimentId}/prices")
    public ResponseEntity<PrixAlimentResponse> create(
            @PathVariable UUID alimentId,
            @Valid @RequestBody CreatePrixAlimentRequest request
    ) {
        PrixAliment prixAliment =
                prixAlimentService.create(
                        new CreatePrixAliment(
                                alimentId,
                                request.prixUnitaire(),
                                request.dateDebut(),
                                request.dateFin()
                        )
                );

        return ResponseEntity.ok(
                PrixAlimentResponse.from(prixAliment)
        );
    }

    @GetMapping("/{alimentId}/prices")
    public ResponseEntity<List<PrixAlimentResponse>> getHistory(
            @PathVariable UUID alimentId
    ) {
        List<PrixAlimentResponse> response =
                prixAlimentService
                        .getHistory(
                                new GetPrixAlimentHistory(alimentId)
                        )
                        .stream()
                        .map(PrixAlimentResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }
}