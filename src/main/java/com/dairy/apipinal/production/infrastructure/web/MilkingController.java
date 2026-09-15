package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.RecordMilking;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lactations/{lactationId}/milkings")
public class MilkingController {

    private final RecordMilking recordMilking;
    private final TenantContext tenantContext;

    public MilkingController(
            RecordMilking recordMilking,
            TenantContext tenantContext
    ) {
        this.recordMilking = recordMilking;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<TraiteResponse> create(
            @PathVariable UUID lactationId,
            @Valid @RequestBody RecordMilkingRequest request
    ) {

        Traite traite = recordMilking.execute(
                new RecordMilking.Command(
                        tenantContext.currentTenantId(),
                        lactationId,
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