package com.dairy.apipinal.animal.infrastructure.web;

import com.dairy.apipinal.animal.application.ChangeAnimalStatus;
import com.dairy.apipinal.animal.application.CreateAnimal;
import com.dairy.apipinal.animal.application.UpdateAnimal;
import com.dairy.apipinal.animal.application.DeleteAnimal;
import com.dairy.apipinal.animal.application.DeclarerMouvementSortie;
import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/animals")
@org.springframework.security.access.prepost.PreAuthorize("@authz.isWorkerOrOwner()")
public class AnimalController {

    private final CreateAnimal createAnimal;
    private final UpdateAnimal updateAnimal;
    private final ChangeAnimalStatus changeAnimalStatus;
    private final DeleteAnimal deleteAnimal;
    private final DeclarerMouvementSortie declarerMouvementSortie;
    private final AnimalRepository animalRepository;
    private final TenantContext tenantContext;

    public AnimalController(
            CreateAnimal createAnimal,
            UpdateAnimal updateAnimal,
            ChangeAnimalStatus changeAnimalStatus,
            DeleteAnimal deleteAnimal,
            DeclarerMouvementSortie declarerMouvementSortie,
            AnimalRepository animalRepository,
            TenantContext tenantContext
    ) {
        this.createAnimal = createAnimal;
        this.updateAnimal = updateAnimal;
        this.changeAnimalStatus = changeAnimalStatus;
        this.deleteAnimal = deleteAnimal;
        this.declarerMouvementSortie = declarerMouvementSortie;
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
                        request.mereId(),
                        request.pereIdentifiant(),
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

    @GetMapping
    public List<AnimalResponse> listByExploitation(@RequestParam UUID exploitationId) {
        return animalRepository
                .findAllByTenantIdAndExploitationIdOrderByIdentifiantAsc(
                        tenantContext.currentTenantId(),
                        exploitationId
                )
                .stream()
                .map(AnimalResponse::from)
                .collect(Collectors.toList());
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
                        request.mereId(),
                        request.pereIdentifiant(),
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

    @PostMapping("/{id}/sortie")
    public ResponseEntity<Void> declarerSortie(
            @PathVariable UUID id,
            @RequestBody MouvementSortieRequest request
    ) {
        Animal animal = animalRepository.findById(id).orElseThrow();
        declarerMouvementSortie.execute(new DeclarerMouvementSortie.Command(
                id,
                tenantContext.currentTenantId(),
                animal.getExploitationId(),
                request.dateSortie(),
                request.motif(),
                request.prixVente(),
                tenantContext.currentUserId()
        ));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{animalId}")
    public ResponseEntity<Void> delete(@PathVariable UUID animalId) {
        deleteAnimal.execute(
                new DeleteAnimal.Command(
                        animalId,
                        tenantContext.currentTenantId(),
                        tenantContext.currentUserId()
                )
        );
        return ResponseEntity.noContent().build();
    }
}