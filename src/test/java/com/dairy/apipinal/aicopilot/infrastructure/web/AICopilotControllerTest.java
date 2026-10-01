package com.dairy.apipinal.aicopilot.infrastructure.web;

import com.dairy.apipinal.aicopilot.api.CopilotJobView;
import com.dairy.apipinal.aicopilot.application.AICopilotService;
import com.dairy.apipinal.aicopilot.application.CopilotFeatureProperties;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.anomaly.api.AnomalyQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AICopilotControllerTest {

    private final AICopilotService service = mock(AICopilotService.class);
    private final AnomalyQueries anomalyQueries = mock(AnomalyQueries.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final CopilotFeatureProperties properties = new CopilotFeatureProperties();
    private final AICopilotController controller =
            new AICopilotController(service, anomalyQueries, tenantContext, properties);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID exploitationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        when(tenantContext.currentTenantId()).thenReturn(tenantId);
        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);
        when(tenantContext.currentUserId()).thenReturn(userId);
    }

    @Test
    void request_uses_server_scope_and_server_side_anomaly() {
        AnomalyAlert anomaly = anomaly("anomaly-1");
        UUID jobId = UUID.randomUUID();
        when(anomalyQueries.detectAnomalies()).thenReturn(List.of(anomaly));
        when(service.requestInterpretation(tenantId, exploitationId, userId, anomaly)).thenReturn(jobId);

        var response = controller.requestInterpretation(anomaly.id());

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(jobId, response.getBody().jobId());
        verify(service).requestInterpretation(tenantId, exploitationId, userId, anomaly);
    }

    @Test
    void job_outside_current_scope_is_not_exposed() {
        UUID foreignJobId = UUID.randomUUID();
        when(anomalyQueries.detectAnomalies()).thenReturn(List.of());
        when(service.findJob(foreignJobId, tenantId, exploitationId, List.of())).thenReturn(Optional.empty());

        var response = controller.getJobStatus(foreignJobId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void latest_returns_typed_validated_contract() {
        AnomalyAlert anomaly = anomaly("anomaly-2");
        CopilotJobView view = new CopilotJobView(
                UUID.randomUUID(), anomaly.id(), "ABSTAINED", "NO_EVIDENCE_FOUND",
                "Aucun élément pertinent antérieur à l’anomalie n’est disponible.", null, null);
        when(anomalyQueries.detectAnomalies()).thenReturn(List.of(anomaly));
        when(service.findLatest(tenantId, exploitationId, anomaly)).thenReturn(Optional.of(view));

        var response = controller.getLatest(anomaly.id());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ABSTAINED", response.getBody().status());
    }

    @Test
    void disabled_capability_remains_discoverable_without_starting_ai() {
        properties.setEnabled(false);
        assertFalse(controller.getCapability().enabled());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> controller.requestInterpretation("anomaly-disabled")).getStatusCode());
        verifyNoInteractions(anomalyQueries);
    }

    private AnomalyAlert anomaly(String id) {
        return new AnomalyAlert(id, "DROP_MILK_PRODUCTION", "AVERTISSEMENT",
                "Baisse", "Baisse observée", UUID.randomUUID(), OffsetDateTime.now());
    }
}
