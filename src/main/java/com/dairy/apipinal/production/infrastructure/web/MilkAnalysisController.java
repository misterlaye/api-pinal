package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.application.EnregistrerAnalyseLait;
import com.dairy.apipinal.production.domain.AnalyseLait;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lactations/{lactationId}/analyses")
@org.springframework.security.access.prepost.PreAuthorize("@authz.isWorkerOrOwner()")
public class MilkAnalysisController {

    private final EnregistrerAnalyseLait enregistrerAnalyseLait;
    private final TenantContext tenantContext;

    public MilkAnalysisController(
            EnregistrerAnalyseLait enregistrerAnalyseLait,
            TenantContext tenantContext
    ) {
        this.enregistrerAnalyseLait = enregistrerAnalyseLait;
        this.tenantContext = tenantContext;
    }

    @PostMapping
    public ResponseEntity<AnalyseLaitResponse> create(
            @PathVariable UUID lactationId,
            @Valid @RequestBody CreateAnalyseLaitRequest request
    ) {
        AnalyseLait analyse = enregistrerAnalyseLait.execute(
                new EnregistrerAnalyseLait.Command(
                        tenantContext.currentTenantId(),
                        lactationId,
                        tenantContext.currentUserId(),
                        request.dateAnalyse(),
                        request.tauxButyreux(),
                        request.tauxProteique(),
                        request.cellulesSomatiques()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AnalyseLaitResponse.from(analyse));
    }
}

record CreateAnalyseLaitRequest(
        @NotNull LocalDate dateAnalyse,
        BigDecimal tauxButyreux,
        BigDecimal tauxProteique,
        Integer cellulesSomatiques
) {
}

record AnalyseLaitResponse(
        UUID id,
        UUID lactationId,
        LocalDate dateAnalyse,
        BigDecimal tauxButyreux,
        BigDecimal tauxProteique,
        Integer cellulesSomatiques,
        OffsetDateTime dateHeureSaisie
) {
    public static AnalyseLaitResponse from(AnalyseLait a) {
        return new AnalyseLaitResponse(
                a.getId(),
                a.getLactationId(),
                a.getDateAnalyse(),
                a.getTauxButyreux(),
                a.getTauxProteique(),
                a.getCellulesSomatiques(),
                a.getDateHeureSaisie()
        );
    }
}
