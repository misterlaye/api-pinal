package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.aicopilot.domain.JobState;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.AIInterpreter;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJob;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJobRepository;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AICopilotWorkerTest {

    private final AnomalyInterpretationJobRepository jobRepository = mock(AnomalyInterpretationJobRepository.class);
    private final EvidenceOrchestrator evidenceOrchestrator = mock(EvidenceOrchestrator.class);
    private final AbstentionEvaluator abstentionEvaluator = mock(AbstentionEvaluator.class);
    private final AIInterpreter aiInterpreter = mock(AIInterpreter.class);
    private final OutputValidator outputValidator = mock(OutputValidator.class);

    private final AICopilotWorker worker = new AICopilotWorker(
            jobRepository, evidenceOrchestrator, abstentionEvaluator, aiInterpreter, outputValidator
    );

    @Test
    void aborts_if_job_not_pending() {
        UUID jobId = UUID.randomUUID();
        AnomalyInterpretationJob job = mock(AnomalyInterpretationJob.class);
        when(job.getStatus()).thenReturn(JobState.COMPLETED);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        worker.processJob(new JobCreatedEvent(jobId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), mock(AnomalyAlert.class)));

        verify(evidenceOrchestrator, never()).gatherContext(any(), any(), any(), any());
    }

    @Test
    void sets_abstained_if_evaluator_says_so() {
        UUID jobId = UUID.randomUUID();
        AnomalyInterpretationJob job = new AnomalyInterpretationJob(jobId, UUID.randomUUID(), UUID.randomUUID(), "anom-1", "fp", "hash");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        EvidenceContext ctx = mock(EvidenceContext.class);
        when(evidenceOrchestrator.gatherContext(any(), any(), any(), any())).thenReturn(ctx);
        when(abstentionEvaluator.evaluate(ctx)).thenReturn(AbstentionEvaluator.AbstentionReason.LOW_SIGNAL);

        worker.processJob(new JobCreatedEvent(jobId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), mock(AnomalyAlert.class)));

        verify(jobRepository, atLeastOnce()).save(job);
        verify(aiInterpreter, never()).interpret(any());
        assertEquals(JobState.ABSTAINED, job.getStatus());
    }

    @Test
    void successful_execution_updates_job_to_completed() {
        UUID jobId = UUID.randomUUID();
        AnomalyInterpretationJob job = new AnomalyInterpretationJob(jobId, UUID.randomUUID(), UUID.randomUUID(), "anom-1", "fp", "hash");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AnomalyAlert anomaly = new AnomalyAlert(
                "anom-1", "DROP_MILK_PRODUCTION", "AVERTISSEMENT", "Baisse", "Baisse observée",
                UUID.randomUUID(), OffsetDateTime.now());
        EvidenceContext ctx = EvidenceContext.build(anomaly, List.of());
        when(evidenceOrchestrator.gatherContext(any(), any(), any(), any())).thenReturn(ctx);
        when(abstentionEvaluator.evaluate(ctx)).thenReturn(AbstentionEvaluator.AbstentionReason.NONE);

        LLMOutput rawOutput = new LLMOutput(List.of(), List.of());
        when(aiInterpreter.interpret(ctx)).thenReturn(rawOutput);
        
        LLMOutput validOutput = new LLMOutput(List.of(), List.of());
        when(outputValidator.validate(rawOutput, ctx)).thenReturn(validOutput);

        worker.processJob(new JobCreatedEvent(jobId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), mock(AnomalyAlert.class)));

        verify(jobRepository, atLeastOnce()).save(job);
        assertEquals(JobState.COMPLETED, job.getStatus());
    }

    @Test
    void exception_updates_job_to_failed() {
        UUID jobId = UUID.randomUUID();
        AnomalyInterpretationJob job = new AnomalyInterpretationJob(jobId, UUID.randomUUID(), UUID.randomUUID(), "anom-1", "fp", "hash");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        when(evidenceOrchestrator.gatherContext(any(), any(), any(), any())).thenThrow(new RuntimeException("Simulated error"));

        worker.processJob(new JobCreatedEvent(jobId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), mock(AnomalyAlert.class)));

        verify(jobRepository, atLeastOnce()).save(job);
        assertEquals(JobState.FAILED, job.getStatus());
    }
}
