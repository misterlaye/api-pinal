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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reproduction")
@PreAuthorize("@authz.isOwner()")
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
    public CycleReproductionResponse declarerInsemination(@Valid @RequestBody InseminationRequest request) {
        UUID tenantId = tenantContext.currentTenantId();
        UUID exploitationId = tenantContext.requireExploitation(request.exploitationId());

        CycleReproduction cycle = declarerInsemination.execute(new DeclarerInsemination.Command(
                request.animalId(),
                tenantId,
                exploitationId,
                request.dateInsemination(),
                parseMethode(request.methodeReproduction()),
                request.taureauId(),
                request.codePaillette(),
                tenantContext.currentUserId()
        ));

        return toResponse(cycle);
    }

    @PostMapping("/cycles/{id}/constat")
    public CycleReproductionResponse enregistrerConstat(
            @PathVariable UUID id,
            @Valid @RequestBody ConstatGestationRequest request
    ) {
        CycleReproduction cycle = enregistrerConstatGestation.execute(new EnregistrerConstatGestation.Command(
                id,
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId(),
                request.dateConstat(),
                request.resultat(),
                request.veterinaire(),
                tenantContext.currentUserId()
        ));

        return toResponse(cycle);
    }

    @PostMapping("/cycles/{id}/velage")
    public CycleReproductionResponse declarerVelage(
            @PathVariable UUID id,
            @Valid @RequestBody VelageRequest request
    ) {
        CycleReproduction cycle = declarerVelage.execute(new DeclarerVelage.Command(
                id,
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId(),
                request.dateReelle(),
                request.veaux() != null
                        ? request.veaux().stream()
                                .map(v -> new VelageEnregistreEvent.VeauPayload(v.identifiant(), v.nom(), v.sexe(), v.indexPortee()))
                                .toList()
                        : java.util.Collections.emptyList(),
                tenantContext.currentUserId()
        ));

        return toResponse(cycle);
    }

    @PostMapping("/cycles/{id}/avortement")
    public CycleReproductionResponse declarerAvortement(
            @PathVariable UUID id
    ) {
        CycleReproduction cycle = declarerAvortement.execute(new DeclarerAvortement.Command(
                id,
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId(),
                tenantContext.currentUserId()
        ));

        return toResponse(cycle);
    }

    @GetMapping("/animaux/{animalId}/cycles")
    public List<CycleReproductionResponse> getCycles(@PathVariable UUID animalId) {
        return reproductionQueries.getCyclesByAnimal(
                animalId,
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId()
        );
    }

    private CycleReproductionResponse toResponse(CycleReproduction cycle) {
        return reproductionQueries.getCycle(cycle.getId(), cycle.getTenantId(), cycle.getExploitationId())
                .orElseThrow(() -> new IllegalStateException("Cycle de reproduction introuvable après enregistrement."));
    }

    private static MethodeReproduction parseMethode(String methode) {
        try {
            return MethodeReproduction.valueOf(methode);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Méthode de reproduction inconnue : " + methode);
        }
    }
}
