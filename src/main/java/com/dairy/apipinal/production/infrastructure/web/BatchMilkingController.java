package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.EnregistrerTraiteLot;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.domain.TypeTraite;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/milkings/batch")
@org.springframework.security.access.prepost.PreAuthorize("@authz.isWorkerOrOwner()")
public class BatchMilkingController {

    private final EnregistrerTraiteLot enregistrerTraiteLot;
    private final TenantContext tenantContext;

    public BatchMilkingController(
            EnregistrerTraiteLot enregistrerTraiteLot,
            TenantContext tenantContext
    ) {
        this.enregistrerTraiteLot = enregistrerTraiteLot;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<List<TraiteResponse>> createBatch(
            @Valid @RequestBody BatchMilkingRequest request
    ) {
        List<EnregistrerTraiteLot.SaisieItem> items = request.items().stream()
                .map(i -> new EnregistrerTraiteLot.SaisieItem(i.animalId(), i.quantiteKg()))
                .collect(Collectors.toList());

        List<Traite> traites = enregistrerTraiteLot.execute(
                new EnregistrerTraiteLot.Command(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId(),
                        tenantContext.currentUserId(),
                        request.dateHeure(),
                        request.type(),
                        items
                )
        );

        List<TraiteResponse> responses = traites.stream()
                .map(TraiteResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responses);
    }
}

record BatchMilkingRequest(
        OffsetDateTime dateHeure,
        @NotNull TypeTraite type,
        @NotEmpty List<BatchMilkingItem> items
) {
}

record BatchMilkingItem(
        @NotNull UUID animalId,
        @NotNull BigDecimal quantiteKg
) {
}
