package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.CopilotJobView;
import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.aicopilot.domain.JobState;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.AIInterpreter;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJob;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJobRepository;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AICopilotProductizationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void double_request_reuses_one_job_and_publishes_one_processing_event() {
        EvidenceOrchestrator orchestrator = mock(EvidenceOrchestrator.class);
        AnomalyInterpretationJobRepository repository = mock(AnomalyInterpretationJobRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        AIInterpreter interpreter = versionedInterpreter();
        AICopilotService service = service(orchestrator, repository, publisher, interpreter);
        AnomalyAlert anomaly = anomaly("same-anomaly");
        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.randomUUID();
        EvidenceContext context = EvidenceContext.build(anomaly, List.of());
        AtomicReference<AnomalyInterpretationJob> persisted = new AtomicReference<>();

        when(orchestrator.gatherContext(any(), any(), any(), any())).thenReturn(context);
        when(repository.findFirstByTenantIdAndExploitationIdAndAnomalyIdOrderByCreatedAtDesc(any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
        when(repository.findByTenantIdAndExploitationIdAndAnomalyIdAndAnalysisFingerprint(any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
        when(repository.save(any())).thenAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return persisted.get();
        });

        UUID first = service.requestInterpretation(tenantId, exploitationId, UUID.randomUUID(), anomaly);
        UUID second = service.requestInterpretation(tenantId, exploitationId, UUID.randomUUID(), anomaly);

        assertEquals(first, second);
        verify(publisher, times(1)).publishEvent(any(JobCreatedEvent.class));
        verify(repository, times(1)).save(any(AnomalyInterpretationJob.class));
    }

    @Test
    void scoped_lookup_refuses_a_job_from_another_tenant() {
        EvidenceOrchestrator orchestrator = mock(EvidenceOrchestrator.class);
        AnomalyInterpretationJobRepository repository = mock(AnomalyInterpretationJobRepository.class);
        AICopilotService service = service(
                orchestrator, repository, mock(ApplicationEventPublisher.class), versionedInterpreter());
        UUID jobId = UUID.randomUUID();
        UUID tenantA = UUID.randomUUID();
        UUID exploitationA = UUID.randomUUID();
        when(repository.findByIdAndTenantIdAndExploitationId(jobId, tenantA, exploitationA))
                .thenReturn(Optional.empty());

        Optional<CopilotJobView> result = service.findJob(jobId, tenantA, exploitationA, List.of());

        assertTrue(result.isEmpty());
        verify(repository).findByIdAndTenantIdAndExploitationId(jobId, tenantA, exploitationA);
        verify(repository, never()).findById(jobId);
    }

    @Test
    void changed_evidence_marks_result_stale_and_hides_old_interpretation() {
        EvidenceOrchestrator orchestrator = mock(EvidenceOrchestrator.class);
        AnomalyInterpretationJobRepository repository = mock(AnomalyInterpretationJobRepository.class);
        AICopilotService service = service(
                orchestrator, repository, mock(ApplicationEventPublisher.class), versionedInterpreter());
        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AnomalyAlert anomaly = anomaly("stale-anomaly");
        AnomalyInterpretationJob job = new AnomalyInterpretationJob(
                jobId, tenantId, exploitationId, UUID.randomUUID(), anomaly.id(), "fingerprint", "old-context");
        job.setStatus(JobState.COMPLETED);
        job.setResultJson("{\"hypotheses\":[],\"suggestedActions\":[],\"limitation\":\"ancienne\"}");
        when(repository.findByIdAndTenantIdAndExploitationId(jobId, tenantId, exploitationId))
                .thenReturn(Optional.of(job));
        EvidenceContext changedContext = EvidenceContext.build(anomaly, List.of(evidence("new-evidence")));
        when(orchestrator.gatherContext(tenantId, exploitationId, anomaly.animalId(), anomaly))
                .thenReturn(changedContext);

        CopilotJobView view = service.findJob(jobId, tenantId, exploitationId, List.of(anomaly)).orElseThrow();

        assertEquals("STALE", view.status());
        assertNull(view.result());
    }

    @Test
    void completed_worker_persists_only_the_validated_product_contract() throws Exception {
        AnomalyInterpretationJobRepository repository = mock(AnomalyInterpretationJobRepository.class);
        EvidenceOrchestrator orchestrator = mock(EvidenceOrchestrator.class);
        AbstentionEvaluator abstention = mock(AbstentionEvaluator.class);
        AIInterpreter interpreter = mock(AIInterpreter.class);
        OutputValidator validator = mock(OutputValidator.class);
        ActionPolicyService actionPolicy = new ActionPolicyService();
        AICopilotWorker worker = new AICopilotWorker(
                repository, orchestrator, abstention, interpreter, validator,
                new CopilotResultAssembler(actionPolicy), objectMapper,
                new CopilotProductMetrics(new SimpleMeterRegistry()));
        UUID jobId = UUID.randomUUID();
        AnomalyAlert anomaly = anomaly("completed-anomaly");
        Evidence evidence = evidence("evidence-1");
        EvidenceContext context = EvidenceContext.build(anomaly, List.of(evidence));
        AnomalyInterpretationJob job = new AnomalyInterpretationJob(
                jobId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), anomaly.id(), "fp", context.contextHash());
        LLMOutput raw = new LLMOutput(
                List.of(new LLMOutput.Hypothesis("Facteur possible.", List.of(evidence.evidenceId()))),
                List.of("CHECK_WATER_SUPPLY"));

        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        when(orchestrator.gatherContext(any(), any(), any(), any())).thenReturn(context);
        when(abstention.evaluate(context)).thenReturn(AbstentionEvaluator.AbstentionReason.NONE);
        when(interpreter.interpret(context)).thenReturn(raw);
        when(validator.validate(raw, context)).thenReturn(raw);

        worker.processJob(new JobCreatedEvent(
                jobId, job.getTenantId(), job.getExploitationId(), anomaly.animalId(), anomaly));

        assertEquals(JobState.COMPLETED, job.getStatus());
        var json = objectMapper.readTree(job.getResultJson());
        assertEquals("Vérifier l’approvisionnement en eau",
                json.at("/suggestedActions/0/label").asText());
        assertEquals("HEALTH", json.at("/hypotheses/0/evidences/0/domain").asText());
        assertFalse(job.getResultJson().contains(evidence.evidenceId()));
    }

    private AICopilotService service(EvidenceOrchestrator orchestrator,
                                     AnomalyInterpretationJobRepository repository,
                                     ApplicationEventPublisher publisher,
                                     AIInterpreter interpreter) {
        return new AICopilotService(
                orchestrator, repository, publisher, interpreter, objectMapper,
                new CopilotProductMetrics(new SimpleMeterRegistry()));
    }

    private AIInterpreter versionedInterpreter() {
        AIInterpreter interpreter = mock(AIInterpreter.class);
        when(interpreter.getPromptVersion()).thenReturn("v1");
        when(interpreter.getModelVersion()).thenReturn("model-v1");
        when(interpreter.getOutputSchemaVersion()).thenReturn("v1");
        return interpreter;
    }

    private AnomalyAlert anomaly(String id) {
        return new AnomalyAlert(id, "DROP_MILK_PRODUCTION", "AVERTISSEMENT",
                "Baisse", "Production en baisse de 12 kg", UUID.randomUUID(), OffsetDateTime.now());
    }

    private Evidence evidence(String id) {
        Evidence evidence = mock(Evidence.class);
        when(evidence.evidenceId()).thenReturn(id);
        when(evidence.contentDigest()).thenReturn("digest-" + id);
        when(evidence.sourceModule()).thenReturn("HEALTH");
        when(evidence.factType()).thenReturn("HEALTH_EVENT");
        when(evidence.observationDate()).thenReturn(OffsetDateTime.now().minusDays(1));
        when(evidence.values()).thenReturn(Map.of("diagnostic", "Observation vétérinaire"));
        when(evidence.supportLevel()).thenReturn(SupportLevel.STRONG);
        when(evidence.temporalRelation()).thenReturn(TemporalRelation.ANTE_ANOMALY);
        return evidence;
    }
}
