package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.GetCourbeLactation;
import com.dairy.apipinal.production.application.RecordMilking;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lactations/{lactationId}/milkings")
public class MilkingController {

    private final RecordMilking recordMilking;
    private final GetCourbeLactation getCourbeLactation;
    private final TenantContext tenantContext;

    public MilkingController(
            RecordMilking recordMilking,
            GetCourbeLactation getCourbeLactation,
            TenantContext tenantContext
    ) {
        this.recordMilking = recordMilking;
        this.getCourbeLactation = getCourbeLactation;
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
                        tenantContext.currentExploitationId(),
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

    @GetMapping("/courbe")
    public ResponseEntity<List<GetCourbeLactation.PointCourbe>> getCourbe(
            @PathVariable UUID lactationId
    ) {
        List<GetCourbeLactation.PointCourbe> courbe = getCourbeLactation.execute(
                tenantContext.currentTenantId(),
                lactationId
        );
        return ResponseEntity.ok(courbe);
    }
}