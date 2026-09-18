ALTER TABLE ration
    ADD COLUMN tenant_id UUID NOT NULL;

CREATE INDEX idx_ration_tenant
    ON ration (tenant_id);

CREATE INDEX idx_ration_tenant_animal
    ON ration (tenant_id, animal_id);