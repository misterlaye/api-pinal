package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.RecordAnimalMilking;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animals/{animalId}/milkings")
@org.springframework.security.access.prepost.PreAuthorize("@authz.isWorkerOrOwner()")
public class AnimalMilkingController {

    private final RecordAnimalMilking recordAnimalMilking;
    private final TenantContext tenantContext;

    public AnimalMilkingController(
            RecordAnimalMilking recordAnimalMilking,
            TenantContext tenantContext
    ) {
        this.recordAnimalMilking = recordAnimalMilking;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<TraiteResponse> create(
            @PathVariable UUID animalId,
            @Valid @RequestBody RecordMilkingRequest request
    ) {
        Traite traite = recordAnimalMilking.execute(
                new RecordAnimalMilking.Command(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId(),
                        animalId,
                        tenantContext.currentUserId(),
                        request.dateHeure(),
                        request.type(),
                        request.quantiteKg()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TraiteResponse.from(traite));
    }
}
