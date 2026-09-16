package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.*;
import com.dairy.apipinal.nutrition.domain.Ration;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animals/{animalId}/rations")
public class RationController {

    private final RationService rationService;

    public RationController(RationService rationService) {
        this.rationService = rationService;
    }

    @PostMapping
    public ResponseEntity<RationResponse> create(
            @PathVariable UUID animalId,
            @Valid @RequestBody CreateRationRequest request
    ) {

        CreateRation command = new CreateRation(
                animalId,
                request.dateDebut(),
                request.origine()
        );

        var ration = rationService.create(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RationResponse.from(ration));
    }

    @PostMapping("/{rationId}/lines")
    public ResponseEntity<RationResponse> addLine(
            @PathVariable UUID animalId,
            @PathVariable UUID rationId,
            @Valid @RequestBody AddRationLineRequest request
    ) {

        AddRationLine command = new AddRationLine(
                rationId,
                animalId,
                request.alimentId(),
                request.quantite()
        );

        Ration ration = rationService.addLine(command);

        return ResponseEntity.ok(RationResponse.from(ration));
    }

    @PostMapping("/{rationId}/activate")
    public ResponseEntity<RationResponse> activate(
            @PathVariable UUID animalId,
            @PathVariable UUID rationId
    ) {

        Ration ration = rationService.activate(animalId, rationId);

        return ResponseEntity.ok(RationResponse.from(ration));
    }

    @PostMapping("/{rationId}/terminate")
    public ResponseEntity<RationResponse> terminate(
            @PathVariable UUID animalId,
            @PathVariable UUID rationId,
            @Valid @RequestBody TerminateRationRequest request
    ) {
        Ration ration = rationService.terminate(
                new TerminateRation(
                        animalId,
                        rationId,
                        request.dateFin()
                )
        );

        return ResponseEntity.ok(
                RationResponse.from(ration)
        );
    }

    @GetMapping("/{rationId}")
    public ResponseEntity<RationResponse> getRation(
            @PathVariable UUID animalId,
            @PathVariable UUID rationId
    ) {
        Ration ration = rationService.getRation(
                new GetRation(rationId)
        );

        if (!ration.getAnimalId().equals(animalId)) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        return ResponseEntity.ok(
                RationResponse.from(ration)
        );
    }
}