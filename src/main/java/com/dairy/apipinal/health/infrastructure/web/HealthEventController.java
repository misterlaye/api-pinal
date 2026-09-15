package com.dairy.apipinal.health.infrastructure.web;

import com.dairy.apipinal.health.application.GetHealthEvent;
import com.dairy.apipinal.health.application.ListAnimalHealthEvents;
import com.dairy.apipinal.health.application.RecordHealthEvent;
import com.dairy.apipinal.health.application.UpdateHealthEvent;
import com.dairy.apipinal.health.domain.EvenementSanitaire;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animals/{animalId}/health-events")
public class HealthEventController {

    private final RecordHealthEvent recordHealthEvent;
    private final GetHealthEvent getHealthEvent;
    private final ListAnimalHealthEvents listAnimalHealthEvents;
    private final UpdateHealthEvent updateHealthEvent;
    private final TenantContext tenantContext;

    public HealthEventController(
            RecordHealthEvent recordHealthEvent,
            GetHealthEvent getHealthEvent,
            ListAnimalHealthEvents listAnimalHealthEvents,
            UpdateHealthEvent updateHealthEvent,
            TenantContext tenantContext
    ) {
        this.recordHealthEvent = recordHealthEvent;
        this.getHealthEvent = getHealthEvent;
        this.listAnimalHealthEvents = listAnimalHealthEvents;
        this.updateHealthEvent = updateHealthEvent;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<HealthEventResponse> create(
            @PathVariable UUID animalId,
            @Valid @RequestBody RecordHealthEventRequest request
    ) {

        EvenementSanitaire event =
                recordHealthEvent.execute(
                        new RecordHealthEvent.Command(
                                tenantContext.currentTenantId(),
                                animalId,
                                request.dateHeure(),
                                request.description(),
                                tenantContext.currentUserId()
                        )
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(HealthEventResponse.from(event));
    }

    @GetMapping
    public List<HealthEventResponse> list(
            @PathVariable UUID animalId
    ) {
        return listAnimalHealthEvents
                .execute(
                        tenantContext.currentTenantId(),
                        animalId
                )
                .stream()
                .map(HealthEventResponse::from)
                .toList();
    }

    @GetMapping("/{eventId}")
    public HealthEventResponse get(
            @PathVariable UUID animalId,
            @PathVariable UUID eventId
    ) {

        EvenementSanitaire event =
                getHealthEvent.execute(tenantContext.currentTenantId(), eventId);

        if (!event.getAnimalId().equals(animalId)) {
            throw new IllegalArgumentException("L'événement sanitaire n'appartient pas à cet animal.");
        }

        return HealthEventResponse.from(event);
    }

    @PatchMapping("/{eventId}")
    public HealthEventResponse update(
            @PathVariable UUID animalId,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateHealthEventRequest request
    ) {

        EvenementSanitaire event =
                updateHealthEvent.execute(
                        new UpdateHealthEvent.Command(
                                eventId,
                                tenantContext.currentTenantId(),
                                request.description(),
                                request.diagnostic(),
                                request.traitement(),
                                request.dateFin(),
                                tenantContext.currentUserId()
                        )
                );

        if (!event.getAnimalId().equals(animalId)) {
            throw new IllegalArgumentException(
                    "L'événement sanitaire n'appartient pas à cet animal."
            );
        }

        return HealthEventResponse.from(event);
    }
}