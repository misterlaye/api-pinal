CREATE TABLE lactation
(
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE,
    statut VARCHAR(30) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_lactation
        PRIMARY KEY (id),

    CONSTRAINT ck_lactation_statut
        CHECK (statut IN ('EN_COURS', 'TERMINEE')),

    CONSTRAINT ck_lactation_dates
        CHECK (
            date_fin IS NULL
                OR date_fin >= date_debut
            )
);

CREATE UNIQUE INDEX uk_lactation_animal_active
    ON lactation (animal_id)
    WHERE statut = 'EN_COURS';

CREATE INDEX idx_lactation_tenant
    ON lactation (tenant_id);

CREATE INDEX idx_lactation_animal
    ON lactation (animal_id);

CREATE TABLE traite
(
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    lactation_id UUID NOT NULL,
    auteur_id UUID NOT NULL,

    date_heure TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(30) NOT NULL,
    quantite_kg NUMERIC(12, 3) NOT NULL,
    date_heure_saisie TIMESTAMP WITH TIME ZONE NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_traite
        PRIMARY KEY (id),

    CONSTRAINT fk_traite_lactation
        FOREIGN KEY (lactation_id)
            REFERENCES lactation(id),

    CONSTRAINT ck_traite_type
        CHECK (type IN ('MATIN', 'SOIR', 'AUTRE')),

    CONSTRAINT ck_traite_quantite
        CHECK (quantite_kg >= 0)
);

CREATE INDEX idx_traite_tenant
    ON traite (tenant_id);

CREATE INDEX idx_traite_lactation
    ON traite (lactation_id);

CREATE INDEX idx_traite_date_heure
    ON traite (date_heure);