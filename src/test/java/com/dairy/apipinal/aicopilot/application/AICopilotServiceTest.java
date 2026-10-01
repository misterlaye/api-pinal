package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.AIInterpreter;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJob;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJobRepository;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AICopilotServiceTest {

    private final EvidenceOrchestrator orchestrator = mock(EvidenceOrchestrator.class);
    private final AnomalyInterpretationJobRepository repository = mock(AnomalyInterpretationJobRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final AIInterpreter interpreter = mock(AIInterpreter.class);

    private final AICopilotService service = new AICopilotService(orchestrator, repository, publisher, interpreter);

    @Test
    void returns_existing_job_if_fingerprint_matches() {
        UUID tenantId = UUID.randomUUID();
        UUID anomalyId = UUID.randomUUID();
        AnomalyAlert anomaly = new AnomalyAlert(anomalyId.toString(), "C", "S", "T", "D", UUID.randomUUID(), OffsetDateTime.now());

        EvidenceContext ctx = EvidenceContext.build(anomaly, List.of());
        when(orchestrator.gatherContext(any(), any(), any(), any())).thenReturn(ctx);
        when(interpreter.getModelVersion()).thenReturn("v1");
        when(interpreter.getPromptVersion()).thenReturn("v1");
        when(interpreter.getOutputSchemaVersion()).thenReturn("v1");

        UUID existingJobId = UUID.randomUUID();
        AnomalyInterpretationJob existingJob = new AnomalyInterpretationJob(existingJobId, tenantId, UUID.randomUUID(), anomaly.id(), "fp", "hash");
        when(repository.findByTenantIdAndExploitationIdAndAnomalyIdAndAnalysisFingerprint(
                eq(tenantId), any(), eq(anomaly.id()), any())).thenReturn(Optional.of(existingJob));

        UUID returnedId = service.requestInterpretation(tenantId, UUID.randomUUID(), UUID.randomUUID(), anomaly);

        assertEquals(existingJobId, returnedId);
        verify(repository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void creates_new_job_and_publishes_event_if_no_match() {
        UUID tenantId = UUID.randomUUID();
        UUID anomalyId = UUID.randomUUID();
        AnomalyAlert anomaly = new AnomalyAlert(anomalyId.toString(), "C", "S", "T", "D", UUID.randomUUID(), OffsetDateTime.now());

        EvidenceContext ctx = EvidenceContext.build(anomaly, List.of());
        when(orchestrator.gatherContext(any(), any(), any(), any())).thenReturn(ctx);
        when(interpreter.getModelVersion()).thenReturn("v1");
        when(interpreter.getPromptVersion()).thenReturn("v1");
        when(interpreter.getOutputSchemaVersion()).thenReturn("v1");

        when(repository.findByTenantIdAndExploitationIdAndAnomalyIdAndAnalysisFingerprint(
                any(), any(), any(), any())).thenReturn(Optional.empty());

        UUID returnedId = service.requestInterpretation(tenantId, UUID.randomUUID(), UUID.randomUUID(), anomaly);

        assertNotNull(returnedId);
        verify(repository, times(1)).save(any());
        verify(publisher, times(1)).publishEvent(any(JobCreatedEvent.class));
    }
}
