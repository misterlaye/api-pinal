package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.GetLactation;
import com.dairy.apipinal.production.application.StartLactation;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lactations")
public class LactationController {

    private final StartLactation startLactation;
    private final GetLactation getLactation;
    private final TenantContext tenantContext;

    public LactationController(
            StartLactation startLactation,
            GetLactation getLactation,
            TenantContext tenantContext
    ) {
        this.startLactation = startLactation;
        this.getLactation = getLactation;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<LactationResponse> create(
            @Valid @RequestBody StartLactationRequest request
    ) {

        Lactation lactation = startLactation.execute(
                new StartLactation.Command(
                        tenantContext.currentTenantId(),
                        request.animalId(),
                        request.dateDebut(),
                        tenantContext.currentUserId()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(LactationResponse.from(lactation));
    }

    @GetMapping("/{lactationId}")
    public LactationResponse get(
            @PathVariable UUID lactationId
    ) {

        return LactationResponse.from(
                getLactation.execute(
                        tenantContext.currentTenantId(),
                        lactationId
                )
        );
    }
}