package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.CreateRation;
import com.dairy.apipinal.nutrition.application.RationService;
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
}