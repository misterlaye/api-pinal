package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.AddRationLine;
import com.dairy.apipinal.nutrition.application.CreateRation;
import com.dairy.apipinal.nutrition.application.RationService;
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
}