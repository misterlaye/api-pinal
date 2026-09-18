package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.application.*;
import com.dairy.apipinal.nutrition.domain.Ration;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping
    public ResponseEntity<RationsPageResponse> getRations(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Le numéro de page ne peut pas être négatif."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "La taille de page doit être comprise entre 1 et 100."
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "dateDebut")
        );

        Page<Ration> rations = rationService.getRationsByAnimal(
                new GetRationsByAnimal(
                        animalId,
                        pageable
                )
        );

        return ResponseEntity.ok(
                RationsPageResponse.from(rations)
        );
    }

    @GetMapping("/{rationId}/cost")
    public ResponseEntity<RationCostResponse> calculateCost(
            @PathVariable UUID animalId,
            @PathVariable UUID rationId,
            @RequestParam LocalDate date
    ) {
        RationCostResult result = rationService.calculateCost(
                new CalculateRationCost(
                        animalId,
                        rationId,
                        date
                )
        );

        return ResponseEntity.ok(
                RationCostResponse.from(result)
        );
    }
}