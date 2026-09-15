CREATE TABLE tenant (
    id UUID PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    statut VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE utilisateur (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    nom VARCHAR(120) NOT NULL,
    prenom VARCHAR(120) NOT NULL,
    statut VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_utilisateur_email
     UNIQUE (email)
);
