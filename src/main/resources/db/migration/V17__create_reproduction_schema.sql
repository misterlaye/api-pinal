-- 1. Enrichir la table animal pour la généalogie
ALTER TABLE animal ADD COLUMN mere_id UUID REFERENCES animal(id);
ALTER TABLE animal ADD COLUMN pere_identifiant VARCHAR(100);

-- 2. Créer la table mouvement_sortie dans le module animal
CREATE TABLE mouvement_sortie (
    id UUID PRIMARY KEY,
    animal_id UUID NOT NULL REFERENCES animal(id),
    tenant_id UUID NOT NULL,
    exploitation_id UUID NOT NULL,
    date_sortie DATE NOT NULL,
    motif VARCHAR(255) NOT NULL,
    prix_vente DECIMAL(12, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NOT NULL
);
CREATE INDEX idx_mouvement_sortie_animal ON mouvement_sortie(animal_id);
CREATE INDEX idx_mouvement_sortie_tenant ON mouvement_sortie(tenant_id);

-- 3. Créer la table cycle_reproduction
CREATE TABLE cycle_reproduction (
    id UUID PRIMARY KEY,
    animal_id UUID NOT NULL REFERENCES animal(id),
    tenant_id UUID NOT NULL,
    exploitation_id UUID NOT NULL,
    numero_cycle INT NOT NULL,
    statut VARCHAR(50) NOT NULL,
    date_insemination DATE NOT NULL,
    methode_reproduction VARCHAR(50) NOT NULL,
    identifiant_taureau VARCHAR(100),
    date_prevue_velage DATE NOT NULL,
    date_reelle_velage DATE,

    -- Value Object ConstatGestation (embedded)
    constat_date DATE,
    constat_resultat VARCHAR(20),
    constat_veterinaire VARCHAR(100),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_cycle_repro_numero UNIQUE(animal_id, numero_cycle)
);
CREATE INDEX idx_cycle_repro_animal ON cycle_reproduction(animal_id);
CREATE INDEX idx_cycle_repro_tenant ON cycle_reproduction(tenant_id);
