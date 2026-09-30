package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.GetLactation;
import com.dairy.apipinal.production.application.StartLactation;
import com.dairy.apipinal.production.application.TerminerLactation;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lactations")
public class LactationController {

    private final StartLactation startLactation;
    private final GetLactation getLactation;
    private final TerminerLactation terminerLactation;
    private final com.dairy.apipinal.production.infrastructure.persistence.LactationRepository lactationRepository;
    private final TenantContext tenantContext;

    public LactationController(
            StartLactation startLactation,
            GetLactation getLactation,
            TerminerLactation terminerLactation,
            com.dairy.apipinal.production.infrastructure.persistence.LactationRepository lactationRepository,
            TenantContext tenantContext
    ) {
        this.startLactation = startLactation;
        this.getLactation = getLactation;
        this.terminerLactation = terminerLactation;
        this.lactationRepository = lactationRepository;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<LactationResponse> create(
            @Valid @RequestBody StartLactationRequest request
    ) {

        Lactation lactation = startLactation.execute(
                new StartLactation.Command(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId(),
                        request.animalId(),
                        null, // cycleId
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

    @PostMapping("/animals/{animalId}/tarir")
    public ResponseEntity<LactationResponse> tarir(
            @PathVariable UUID animalId,
            @RequestBody(required = false) TarirRequest request
    ) {
        LocalDate dateFin = (request != null && request.dateFin() != null)
                ? request.dateFin()
                : LocalDate.now();

        Lactation lactation = terminerLactation.execute(
                new TerminerLactation.Command(
                        tenantContext.currentTenantId(),
                        animalId,
                        dateFin,
                        tenantContext.currentUserId()
                )
        );

        return ResponseEntity.ok(LactationResponse.from(lactation));
    }

    @GetMapping("/animals/{animalId}/active")
    public ResponseEntity<LactationResponse> getActiveLactation(@PathVariable UUID animalId) {
        return lactationRepository.findByAnimalIdAndTenantIdAndStatut(
                        animalId,
                        tenantContext.currentTenantId(),
                        com.dairy.apipinal.production.domain.StatutLactation.EN_COURS
                )
                .map(LactationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record TarirRequest(LocalDate dateFin) {}
}