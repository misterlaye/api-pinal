package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.CopilotJobView;
import com.dairy.apipinal.aicopilot.api.CopilotResult;
import com.dairy.apipinal.aicopilot.domain.AnalysisFingerprint;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.aicopilot.domain.JobState;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.AIInterpreter;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJob;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJobRepository;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AICopilotService {

    private final EvidenceOrchestrator evidenceOrchestrator;
    private final AnomalyInterpretationJobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AIInterpreter aiInterpreter;
    private final ObjectMapper objectMapper;
    private final CopilotProductMetrics metrics;

    @Autowired
    public AICopilotService(EvidenceOrchestrator evidenceOrchestrator,
                            AnomalyInterpretationJobRepository jobRepository,
                            ApplicationEventPublisher eventPublisher,
                            AIInterpreter aiInterpreter,
                            ObjectMapper objectMapper,
                            CopilotProductMetrics metrics) {
        this.evidenceOrchestrator = evidenceOrchestrator;
        this.jobRepository = jobRepository;
        this.eventPublisher = eventPublisher;
        this.aiInterpreter = aiInterpreter;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    AICopilotService(EvidenceOrchestrator evidenceOrchestrator,
                     AnomalyInterpretationJobRepository jobRepository,
                     ApplicationEventPublisher eventPublisher,
                     AIInterpreter aiInterpreter) {
        this(evidenceOrchestrator, jobRepository, eventPublisher, aiInterpreter,
                new ObjectMapper().findAndRegisterModules(),
                new CopilotProductMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    }

    @Transactional
    public UUID requestInterpretation(UUID tenantId, UUID exploitationId, UUID requestedByUserId,
                                      AnomalyAlert anomaly) {
        EvidenceContext context = evidenceOrchestrator.gatherContext(
                tenantId, exploitationId, anomaly.animalId(), anomaly
        );
        AnalysisFingerprint fingerprint = fingerprint(tenantId, anomaly, context);

        Optional<AnomalyInterpretationJob> latest = jobRepository
                .findFirstByTenantIdAndExploitationIdAndAnomalyIdOrderByCreatedAtDesc(
                        tenantId, exploitationId, anomaly.id());
        metrics.requested(latest.isPresent() && !latest.get().getContextHash().equals(context.contextHash()));

        Optional<AnomalyInterpretationJob> existingJob = jobRepository
                .findByTenantIdAndExploitationIdAndAnomalyIdAndAnalysisFingerprint(
                        tenantId, exploitationId, anomaly.id(), fingerprint.value());

        if (existingJob.isPresent()) {
            AnomalyInterpretationJob existing = existingJob.get();
            if (existing.getStatus() == JobState.FAILED) {
                existing.resetForRetry();
                jobRepository.save(existing);
                publish(existing.getId(), tenantId, exploitationId, anomaly);
            }
            return existing.getId();
        }

        UUID jobId = UUID.randomUUID();
        AnomalyInterpretationJob job = new AnomalyInterpretationJob(
                jobId, tenantId, exploitationId, requestedByUserId,
                anomaly.id(), fingerprint.value(), context.contextHash()
        );
        jobRepository.save(job);
        publish(jobId, tenantId, exploitationId, anomaly);
        return jobId;
    }

    @Transactional(readOnly = true)
    public Optional<CopilotJobView> findJob(UUID jobId, UUID tenantId, UUID exploitationId,
                                            List<AnomalyAlert> currentAnomalies) {
        return jobRepository.findByIdAndTenantIdAndExploitationId(jobId, tenantId, exploitationId)
                .map(job -> toView(job, findCurrentAnomaly(job.getAnomalyId(), currentAnomalies)));
    }

    @Transactional(readOnly = true)
    public Optional<CopilotJobView> findLatest(UUID tenantId, UUID exploitationId,
                                               AnomalyAlert currentAnomaly) {
        return jobRepository.findFirstByTenantIdAndExploitationIdAndAnomalyIdOrderByCreatedAtDesc(
                        tenantId, exploitationId, currentAnomaly.id())
                .map(job -> toView(job, Optional.of(currentAnomaly)));
    }

    private AnalysisFingerprint fingerprint(UUID tenantId, AnomalyAlert anomaly, EvidenceContext context) {
        return AnalysisFingerprint.compute(
                tenantId, anomaly.id(), context.contextHash(), "rules-v1",
                aiInterpreter.getPromptVersion(), aiInterpreter.getModelVersion(), aiInterpreter.getOutputSchemaVersion()
        );
    }

    private void publish(UUID jobId, UUID tenantId, UUID exploitationId, AnomalyAlert anomaly) {
        eventPublisher.publishEvent(new JobCreatedEvent(
                jobId, tenantId, exploitationId, anomaly.animalId(), anomaly));
    }

    private Optional<AnomalyAlert> findCurrentAnomaly(String anomalyId, List<AnomalyAlert> anomalies) {
        return anomalies.stream().filter(anomaly -> anomaly.id().equals(anomalyId)).findFirst();
    }

    private CopilotJobView toView(AnomalyInterpretationJob job, Optional<AnomalyAlert> currentAnomaly) {
        String status = job.getStatus().name();
        if (isTerminal(job.getStatus()) && currentAnomaly.isPresent()) {
            EvidenceContext currentContext = evidenceOrchestrator.gatherContext(
                    job.getTenantId(), job.getExploitationId(), currentAnomaly.get().animalId(), currentAnomaly.get());
            if (!job.getContextHash().equals(currentContext.contextHash())) {
                status = "STALE";
            }
        }

        CopilotResult result = null;
        if ("COMPLETED".equals(status) && job.getResultJson() != null) {
            try {
                result = objectMapper.readValue(job.getResultJson(), CopilotResult.class);
            } catch (JsonProcessingException exception) {
                status = "FAILED";
            }
        }

        String reason = job.getAbstentionReason();
        return new CopilotJobView(
                job.getId(), job.getAnomalyId(), status, reason, abstentionMessage(reason), result, job.getGeneratedAt());
    }

    private boolean isTerminal(JobState status) {
        return status == JobState.COMPLETED || status == JobState.ABSTAINED;
    }

    private String abstentionMessage(String reason) {
        if ("NO_EVIDENCE_FOUND".equals(reason)) {
            return "Aucun élément pertinent antérieur à l’anomalie n’est disponible.";
        }
        if ("LOW_SIGNAL".equals(reason)) {
            return "Les éléments disponibles présentent un niveau de support trop faible.";
        }
        return null;
    }
}
