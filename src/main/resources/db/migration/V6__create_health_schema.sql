CREATE TABLE evenement_sanitaire
(
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    animal_id UUID NOT NULL,

    date_heure TIMESTAMP WITH TIME ZONE NOT NULL,
    description VARCHAR(2000) NOT NULL,
    diagnostic VARCHAR(2000),
    traitement VARCHAR(2000),
    date_fin DATE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_evenement_sanitaire
        PRIMARY KEY (id),

    CONSTRAINT ck_evenement_sanitaire_date_fin
        CHECK (
            date_fin IS NULL
                OR date_fin >= date_heure::date
)
    );

CREATE INDEX idx_evenement_sanitaire_tenant
    ON evenement_sanitaire (tenant_id);

CREATE INDEX idx_evenement_sanitaire_animal
    ON evenement_sanitaire (animal_id);

CREATE INDEX idx_evenement_sanitaire_animal_date
    ON evenement_sanitaire (animal_id, date_heure DESC);