-- 1. Idempotence Lactation
ALTER TABLE lactation ADD COLUMN cycle_reproduction_id UUID REFERENCES cycle_reproduction(id);
CREATE UNIQUE INDEX uk_lactation_cycle ON lactation(cycle_reproduction_id) WHERE cycle_reproduction_id IS NOT NULL;

-- 2. Idempotence EvenementSanitaire (Avortement)
ALTER TABLE evenement_sanitaire ADD COLUMN source_cycle_id UUID REFERENCES cycle_reproduction(id);
CREATE UNIQUE INDEX uk_sante_source_cycle ON evenement_sanitaire(source_cycle_id) WHERE source_cycle_id IS NOT NULL;
