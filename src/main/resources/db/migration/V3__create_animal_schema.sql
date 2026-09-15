CREATE TABLE race
(
    id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    libelle VARCHAR(150) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_race
        PRIMARY KEY (id),

    CONSTRAINT uk_race_code
        UNIQUE (code)
);

CREATE TABLE animal
(
    id UUID NOT NULL,

    tenant_id UUID NOT NULL,
    exploitation_id UUID NOT NULL,
    race_id UUID NOT NULL,

    identifiant VARCHAR(100) NOT NULL,
    nom VARCHAR(150) NOT NULL,
    photo_url VARCHAR(500),
    date_naissance DATE,
    statut VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_animal
        PRIMARY KEY (id),

    CONSTRAINT fk_animal_race
        FOREIGN KEY (race_id)
            REFERENCES race(id),

    CONSTRAINT uk_animal_exploitation_identifiant
        UNIQUE (exploitation_id, identifiant),

    CONSTRAINT ck_animal_statut
        CHECK (statut IN ('ACTIF', 'VENDU', 'DECEDE'))
);

CREATE INDEX idx_race_actif
    ON race (actif);

CREATE INDEX idx_animal_tenant
    ON animal (tenant_id);

CREATE INDEX idx_animal_exploitation
    ON animal (exploitation_id);

CREATE INDEX idx_animal_exploitation_statut
    ON animal (exploitation_id, statut);

CREATE INDEX idx_animal_race
    ON animal (race_id);