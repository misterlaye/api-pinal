-- =====================================================================
-- V14 : Module Identity — Exploitation & Membership
-- =====================================================================
-- La table 'tenant' (V1) représente le propriétaire / organisation.
-- La table 'utilisateur' (V1) représente les utilisateurs.
-- On ajoute ici :
--   - 'exploitation' : une ferme rattachée à un tenant
--   - 'membership'   : lien utilisateur <-> exploitation avec rôle
--   - colonne 'telephone' sur utilisateur (identifiant principal OTP)
-- =====================================================================

-- Ajout du téléphone comme identifiant principal (passwordless OTP)
ALTER TABLE utilisateur
    ADD COLUMN telephone VARCHAR(20);

-- Rendre le téléphone unique (un seul compte par numéro)
ALTER TABLE utilisateur
    ADD CONSTRAINT uk_utilisateur_telephone UNIQUE (telephone);

-- L'email n'est plus obligatoire (le téléphone le remplace)
ALTER TABLE utilisateur
    ALTER COLUMN email DROP NOT NULL;

-- =====================================================================
-- Table exploitation : une ferme / site d'élevage rattaché à un tenant
-- =====================================================================
CREATE TABLE exploitation (
    id         UUID PRIMARY KEY,
    tenant_id  UUID         NOT NULL REFERENCES tenant(id),
    nom        VARCHAR(200) NOT NULL,
    localite   VARCHAR(200),
    actif      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version    BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_exploitation_tenant
    ON exploitation (tenant_id);

-- =====================================================================
-- Table membership : rattachement utilisateur <-> exploitation + rôle
-- =====================================================================
CREATE TABLE membership (
    id               UUID PRIMARY KEY,
    utilisateur_id   UUID        NOT NULL REFERENCES utilisateur(id),
    exploitation_id  UUID        NOT NULL REFERENCES exploitation(id),
    role             VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uk_membership_user_exploitation
        UNIQUE (utilisateur_id, exploitation_id)
);

CREATE INDEX idx_membership_utilisateur
    ON membership (utilisateur_id);

CREATE INDEX idx_membership_exploitation
    ON membership (exploitation_id);
