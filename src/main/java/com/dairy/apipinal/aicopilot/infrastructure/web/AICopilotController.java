package com.dairy.apipinal.aicopilot.infrastructure.web;

import com.dairy.apipinal.aicopilot.api.CopilotJobView;
import com.dairy.apipinal.aicopilot.application.AICopilotService;
import com.dairy.apipinal.aicopilot.application.CopilotFeatureProperties;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.anomaly.api.AnomalyQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/anomalies")
@PreAuthorize("@authz.isOwner()")
public class AICopilotController {

    private final AICopilotService aiCopilotService;
    private final AnomalyQueries anomalyQueries;
    private final TenantContext tenantContext;
    private final CopilotFeatureProperties featureProperties;

    public AICopilotController(AICopilotService aiCopilotService,
                               AnomalyQueries anomalyQueries,
                               TenantContext tenantContext,
                               CopilotFeatureProperties featureProperties) {
        this.aiCopilotService = aiCopilotService;
        this.anomalyQueries = anomalyQueries;
        this.tenantContext = tenantContext;
        this.featureProperties = featureProperties;
    }

    @GetMapping("/ai-capability")
    public CapabilityResponse getCapability() {
        return new CapabilityResponse(featureProperties.isEnabled());
    }

    @PostMapping("/{anomalyId}/interpretations")
    public ResponseEntity<InterpretationJobResponse> requestInterpretation(
            @PathVariable String anomalyId
    ) {
        requireEnabled();
        AnomalyAlert anomaly = requireCurrentAnomaly(anomalyId);
        UUID jobId = aiCopilotService.requestInterpretation(
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId(),
                tenantContext.currentUserId(),
                anomaly
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new InterpretationJobResponse(jobId, "PENDING"));
    }

    @GetMapping("/{anomalyId}/interpretations/latest")
    public ResponseEntity<CopilotJobView> getLatest(@PathVariable String anomalyId) {
        requireEnabled();
        AnomalyAlert anomaly = requireCurrentAnomaly(anomalyId);
        return aiCopilotService.findLatest(
                        tenantContext.currentTenantId(), tenantContext.currentExploitationId(), anomaly)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/interpretations/jobs/{jobId}")
    public ResponseEntity<CopilotJobView> getJobStatus(@PathVariable UUID jobId) {
        requireEnabled();
        List<AnomalyAlert> currentAnomalies = anomalyQueries.detectAnomalies();
        return aiCopilotService.findJob(
                        jobId,
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId(),
                        currentAnomalies)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private AnomalyAlert requireCurrentAnomaly(String anomalyId) {
        return anomalyQueries.detectAnomalies().stream()
                .filter(anomaly -> anomaly.id().equals(anomalyId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anomalie introuvable."));
    }

    private void requireEnabled() {
        if (!featureProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public record CapabilityResponse(boolean enabled) {}
    public record InterpretationJobResponse(UUID jobId, String status) {}
}
