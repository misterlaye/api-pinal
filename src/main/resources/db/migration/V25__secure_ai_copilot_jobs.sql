ALTER TABLE ai_interpretation_job
    ADD COLUMN requested_by_user_id UUID;

CREATE INDEX idx_ai_job_scope
    ON ai_interpretation_job(tenant_id, exploitation_id, anomaly_id, created_at DESC);
