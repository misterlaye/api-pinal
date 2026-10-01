package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.aicopilot.domain.JobState;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.AIInterpreter;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJob;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;

@Component
public class AICopilotWorker {

    private static final Logger log = LoggerFactory.getLogger(AICopilotWorker.class);

    private final AnomalyInterpretationJobRepository jobRepository;
    private final EvidenceOrchestrator evidenceOrchestrator;
    private final AbstentionEvaluator abstentionEvaluator;
    private final AIInterpreter aiInterpreter;
    private final OutputValidator outputValidator;
    private final CopilotResultAssembler resultAssembler;
    private final ObjectMapper objectMapper;
    private final CopilotProductMetrics metrics;

    @Autowired
    public AICopilotWorker(AnomalyInterpretationJobRepository jobRepository,
                           EvidenceOrchestrator evidenceOrchestrator,
                           AbstentionEvaluator abstentionEvaluator,
                           AIInterpreter aiInterpreter,
                           OutputValidator outputValidator,
                           CopilotResultAssembler resultAssembler,
                           ObjectMapper objectMapper,
                           CopilotProductMetrics metrics) {
        this.jobRepository = jobRepository;
        this.evidenceOrchestrator = evidenceOrchestrator;
        this.abstentionEvaluator = abstentionEvaluator;
        this.aiInterpreter = aiInterpreter;
        this.outputValidator = outputValidator;
        this.resultAssembler = resultAssembler;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    public AICopilotWorker(AnomalyInterpretationJobRepository jobRepository,
                           EvidenceOrchestrator evidenceOrchestrator,
                           AbstentionEvaluator abstentionEvaluator,
                           AIInterpreter aiInterpreter,
                           OutputValidator outputValidator) {
        this(jobRepository, evidenceOrchestrator, abstentionEvaluator, aiInterpreter, outputValidator,
                new CopilotResultAssembler(new ActionPolicyService()),
                new ObjectMapper().findAndRegisterModules(),
                new CopilotProductMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processJob(JobCreatedEvent event) {
        AnomalyInterpretationJob job = jobRepository.findById(event.jobId()).orElse(null);
        if (job == null || job.getStatus() != JobState.PENDING) {
            return; // Job deleted or already processed
        }

        try {
            job.setStatus(JobState.IN_PROGRESS);
            jobRepository.saveAndFlush(job);

            // 1. Gather context (Worker rebuilds its scope using event.tenantId())
            EvidenceContext context = evidenceOrchestrator.gatherContext(
                    event.tenantId(), event.exploitationId(), event.animalId(), event.anomaly()
            );

            // 2. Evaluate abstention
            AbstentionEvaluator.AbstentionReason abstentionReason = abstentionEvaluator.evaluate(context);
            if (abstentionReason != AbstentionEvaluator.AbstentionReason.NONE) {
                job.setStatus(JobState.ABSTAINED);
                job.setAbstentionReason(abstentionReason.name());
                jobRepository.save(job);
                metrics.completed(JobState.ABSTAINED.name());
                return;
            }

            // 3. Call LLM Gateway
            LLMOutput rawOutput = aiInterpreter.interpret(context);

            // 4. Validate output
            LLMOutput validatedOutput = outputValidator.validate(rawOutput, context);

            // 5. Success
            job.setStatus(JobState.COMPLETED);
            job.setResultJson(serializeResult(resultAssembler.assemble(validatedOutput, context)));
            job.setGeneratedAt(OffsetDateTime.now());
            job.setModelVersion(aiInterpreter.getModelVersion());
            job.setPromptVersion(aiInterpreter.getPromptVersion());
            jobRepository.save(job);
            metrics.completed(JobState.COMPLETED.name());

        } catch (Exception e) {
            job.setStatus(JobState.FAILED);
            jobRepository.save(job);
            metrics.completed(JobState.FAILED.name());
            log.warn("Échec du job Copilot {} sans exposition du contexte métier", job.getId());
        }
    }

    private String serializeResult(com.dairy.apipinal.aicopilot.api.CopilotResult result)
            throws JsonProcessingException {
        return objectMapper.writeValueAsString(result);
    }
}
