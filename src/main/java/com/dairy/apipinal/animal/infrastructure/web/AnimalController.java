package com.dairy.apipinal.animal.infrastructure.web;

import com.dairy.apipinal.animal.application.ChangeAnimalStatus;
import com.dairy.apipinal.animal.application.CreateAnimal;
import com.dairy.apipinal.animal.application.UpdateAnimal;
import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animals")
public class AnimalController {

    private final CreateAnimal createAnimal;
    private final UpdateAnimal updateAnimal;
    private final ChangeAnimalStatus changeAnimalStatus;
    private final AnimalRepository animalRepository;
    private final TenantContext tenantContext;

    public AnimalController(
            CreateAnimal createAnimal,
            UpdateAnimal updateAnimal,
            ChangeAnimalStatus changeAnimalStatus,
            AnimalRepository animalRepository,
            TenantContext tenantContext
    ) {
        this.createAnimal = createAnimal;
        this.updateAnimal = updateAnimal;
        this.changeAnimalStatus = changeAnimalStatus;
        this.animalRepository = animalRepository;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<AnimalResponse> create(
            @Valid @RequestBody CreateAnimalRequest request
    ) {

        Animal animal = createAnimal.execute(
                new CreateAnimal.Command(
                        tenantContext.currentTenantId(),
                        request.exploitationId(),
                        request.raceId(),
                        request.identifiant(),
                        request.nom(),
                        request.photoUrl(),
                        request.dateNaissance(),
                        tenantContext.currentUserId()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AnimalResponse.from(animal));
    }

    @GetMapping("/{animalId}")
    public AnimalResponse get(@PathVariable UUID animalId) {

        return animalRepository
                .findByIdAndTenantId(
                        animalId,
                        tenantContext.currentTenantId()
                )
                .map(AnimalResponse::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal introuvable."
                ));
    }

    @PatchMapping("/{animalId}")
    public AnimalResponse update(
            @PathVariable UUID animalId,
            @Valid @RequestBody UpdateAnimalRequest request
    ) {

        Animal animal = updateAnimal.execute(
                new UpdateAnimal.Command(
                        animalId,
                        tenantContext.currentTenantId(),
                        request.raceId(),
                        request.identifiant(),
                        request.nom(),
                        request.photoUrl(),
                        request.dateNaissance(),
                        tenantContext.currentUserId()
                )
        );

        return AnimalResponse.from(animal);
    }

    @PatchMapping("/{animalId}/status")
    public AnimalResponse changeStatus(
            @PathVariable UUID animalId,
            @RequestParam StatutAnimal status
    ) {

        Animal animal = changeAnimalStatus.execute(
                new ChangeAnimalStatus.Command(
                        animalId,
                        tenantContext.currentTenantId(),
                        status,
                        tenantContext.currentUserId()
                )
        );

        return AnimalResponse.from(animal);
    }
}