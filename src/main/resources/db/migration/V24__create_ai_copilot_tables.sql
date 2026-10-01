CREATE TABLE ai_interpretation_job (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    exploitation_id UUID NOT NULL,
    anomaly_id VARCHAR(255) NOT NULL,
    analysis_fingerprint VARCHAR(64) NOT NULL,
    context_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    abstention_reason VARCHAR(50),
    result_json JSONB,
    generated_at TIMESTAMP WITH TIME ZONE,
    model_version VARCHAR(50),
    prompt_version VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uk_ai_job_fingerprint ON ai_interpretation_job(tenant_id, anomaly_id, analysis_fingerprint);
CREATE INDEX idx_ai_job_anomaly ON ai_interpretation_job(tenant_id, anomaly_id);
