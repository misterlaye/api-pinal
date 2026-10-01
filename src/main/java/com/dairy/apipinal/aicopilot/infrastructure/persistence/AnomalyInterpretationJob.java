package com.dairy.apipinal.aicopilot.infrastructure.persistence;

import com.dairy.apipinal.aicopilot.domain.JobState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_interpretation_job")
public class AnomalyInterpretationJob {

    @Id
    private UUID id;

    private UUID tenantId;
    private UUID exploitationId;
    private UUID requestedByUserId;
    private String anomalyId;
    
    private String analysisFingerprint;
    private String contextHash;
    
    @Enumerated(EnumType.STRING)
    private JobState status;
    
    private String abstentionReason;
    
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String resultJson;
    
    private String modelVersion;
    private String promptVersion;
    private OffsetDateTime generatedAt;
    
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    // Constructors, Getters, Setters

    protected AnomalyInterpretationJob() {}

    public AnomalyInterpretationJob(UUID id, UUID tenantId, UUID exploitationId,
                                    String anomalyId, String analysisFingerprint, String contextHash) {
        this(id, tenantId, exploitationId, null, anomalyId, analysisFingerprint, contextHash);
    }

    public AnomalyInterpretationJob(UUID id, UUID tenantId, UUID exploitationId, UUID requestedByUserId,
                                    String anomalyId, String analysisFingerprint, String contextHash) {
        this.id = id;
        this.tenantId = tenantId;
        this.exploitationId = exploitationId;
        this.requestedByUserId = requestedByUserId;
        this.anomalyId = anomalyId;
        this.analysisFingerprint = analysisFingerprint;
        this.contextHash = contextHash;
        this.status = JobState.PENDING;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getExploitationId() { return exploitationId; }
    public UUID getRequestedByUserId() { return requestedByUserId; }
    public String getAnomalyId() { return anomalyId; }
    public String getAnalysisFingerprint() { return analysisFingerprint; }
    public String getContextHash() { return contextHash; }
    public JobState getStatus() { return status; }
    public String getAbstentionReason() { return abstentionReason; }
    public String getResultJson() { return resultJson; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setStatus(JobState status) { this.status = status; }
    public void setAbstentionReason(String abstentionReason) { this.abstentionReason = abstentionReason; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public void resetForRetry() {
        this.status = JobState.PENDING;
        this.abstentionReason = null;
        this.resultJson = null;
        this.generatedAt = null;
    }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
}
