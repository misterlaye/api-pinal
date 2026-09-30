package com.dairy.apipinal.reproduction.infrastructure.web;

import com.dairy.apipinal.shared.security.TenantContext;
import com.dairy.apipinal.reproduction.api.CycleReproductionResponse;
import com.dairy.apipinal.reproduction.api.ReproductionQueries;
import com.dairy.apipinal.reproduction.api.VelageEnregistreEvent;
import com.dairy.apipinal.reproduction.application.DeclarerInsemination;
import com.dairy.apipinal.reproduction.application.DeclarerVelage;
import com.dairy.apipinal.reproduction.application.EnregistrerConstatGestation;
import com.dairy.apipinal.reproduction.application.DeclarerAvortement;
import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.domain.MethodeReproduction;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reproduction")
@PreAuthorize("@authz.isWorkerOrOwner()")
public class ReproductionController {

    private final DeclarerInsemination declarerInsemination;
    private final EnregistrerConstatGestation enregistrerConstatGestation;
    private final DeclarerVelage declarerVelage;
    private final DeclarerAvortement declarerAvortement;
    private final ReproductionQueries reproductionQueries;
    private final TenantContext tenantContext;

    public ReproductionController(
            DeclarerInsemination declarerInsemination,
            EnregistrerConstatGestation enregistrerConstatGestation,
            DeclarerVelage declarerVelage,
            DeclarerAvortement declarerAvortement,
            ReproductionQueries reproductionQueries,
            TenantContext tenantContext
    ) {
        this.declarerInsemination = declarerInsemination;
        this.enregistrerConstatGestation = enregistrerConstatGestation;
        this.declarerVelage = declarerVelage;
        this.declarerAvortement = declarerAvortement;
        this.reproductionQueries = reproductionQueries;
        this.tenantContext = tenantContext;
    }

    @PostMapping("/inseminations")
    @ResponseStatus(HttpStatus.CREATED)
    public CycleReproductionResponse declarerInsemination(@RequestBody InseminationRequest request) {
        UUID tenantId = tenantContext.currentTenantId();
        UUID userId = tenantContext.currentUserId();

        CycleReproduction cycle = declarerInsemination.execute(new DeclarerInsemination.Command(
                request.animalId(),
                tenantId,
                request.exploitationId(),
                request.dateInsemination(),
                MethodeReproduction.valueOf(request.methodeReproduction()),
                request.taureauId(),
                request.codePaillette(),
                userId
        ));

        // Let's refetch it via queries to get the properly mapped DTO
        return reproductionQueries.getCyclesByAnimal(request.animalId())
                .stream()
                .filter(c -> c.id().equals(cycle.getId()))
                .findFirst()
                .orElseThrow();
    }

    @PostMapping("/cycles/{id}/constat")
    public CycleReproductionResponse enregistrerConstat(
            @PathVariable UUID id,
            @RequestBody ConstatGestationRequest request
    ) {
        UUID userId = tenantContext.currentUserId();

        CycleReproduction cycle = enregistrerConstatGestation.execute(new EnregistrerConstatGestation.Command(
                id,
                request.dateConstat(),
                request.resultat(),
                request.veterinaire(),
                userId
        ));

        return reproductionQueries.getCyclesByAnimal(cycle.getAnimalId())
                .stream()
                .filter(c -> c.id().equals(cycle.getId()))
                .findFirst()
                .orElseThrow();
    }

    @PostMapping("/cycles/{id}/velage")
    public CycleReproductionResponse declarerVelage(
            @PathVariable UUID id,
            @RequestBody VelageRequest request
    ) {
        UUID userId = tenantContext.currentUserId();

        CycleReproduction cycle = declarerVelage.execute(new DeclarerVelage.Command(
                id,
                request.dateReelle(),
                request.veaux() != null ? request.veaux().stream().map(v -> new VelageEnregistreEvent.VeauPayload(v.identifiant(), v.nom(), v.sexe(), v.indexPortee())).toList() : java.util.Collections.emptyList(),
                userId
        ));

        return reproductionQueries.getCyclesByAnimal(cycle.getAnimalId())
                .stream()
                .filter(c -> c.id().equals(cycle.getId()))
                .findFirst()
                .orElseThrow();
    }

    @PostMapping("/cycles/{id}/avortement")
    public CycleReproductionResponse declarerAvortement(
            @PathVariable UUID id
    ) {
        UUID userId = tenantContext.currentUserId();

        CycleReproduction cycle = declarerAvortement.execute(new DeclarerAvortement.Command(
                id,
                userId
        ));

        return reproductionQueries.getCyclesByAnimal(cycle.getAnimalId())
                .stream()
                .filter(c -> c.id().equals(cycle.getId()))
                .findFirst()
                .orElseThrow();
    }

    @GetMapping("/animaux/{animalId}/cycles")
    public List<CycleReproductionResponse> getCycles(@PathVariable UUID animalId) {
        return reproductionQueries.getCyclesByAnimal(animalId);
    }
}
