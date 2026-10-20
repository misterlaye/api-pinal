package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.EnregistrerTraiteLot;
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

    /**
     * Enregistre les lignes valides et renvoie les lignes refusées avec leur motif.
     * 201 si au moins une traite est enregistrée, 422 si aucune ne l'est.
     */
    @PostMapping
    public ResponseEntity<BatchMilkingResponse> createBatch(
            @Valid @RequestBody BatchMilkingRequest request
    ) {
        EnregistrerTraiteLot.Resultat resultat = enregistrerTraiteLot.execute(
                new EnregistrerTraiteLot.Command(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId(),
                        tenantContext.currentUserId(),
                        request.dateHeure(),
                        request.type(),
                        request.items().stream()
                                .map(i -> new EnregistrerTraiteLot.SaisieItem(i.animalId(), i.quantiteLitres()))
                                .toList()
                )
        );

        BatchMilkingResponse body = new BatchMilkingResponse(
                resultat.enregistrees().stream().map(TraiteResponse::from).toList(),
                resultat.rejets().stream().map(r -> new BatchMilkingResponse.Rejet(r.animalId(), r.motif())).toList()
        );

        return ResponseEntity
                .status(resultat.enregistrees().isEmpty() ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.CREATED)
                .body(body);
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
        @NotNull BigDecimal quantiteLitres
) {
}

record BatchMilkingResponse(
        List<TraiteResponse> enregistrees,
        List<Rejet> rejets
) {
    record Rejet(UUID animalId, String motif) {
    }
}
