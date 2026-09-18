CREATE TABLE prix_vente_lait (
                                 id UUID PRIMARY KEY,
                                 tenant_id UUID NOT NULL,
                                 prix_par_litre NUMERIC(19, 4) NOT NULL,
                                 date_debut DATE NOT NULL,
                                 date_fin DATE,
                                 version BIGINT NOT NULL DEFAULT 0,

                                 CONSTRAINT ck_prix_vente_lait_prix_positif
                                     CHECK (prix_par_litre > 0),

                                 CONSTRAINT ck_prix_vente_lait_periode_valide
                                     CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE INDEX idx_prix_vente_lait_tenant
    ON prix_vente_lait (tenant_id);

CREATE INDEX idx_prix_vente_lait_tenant_date
    ON prix_vente_lait (tenant_id, date_debut);

CREATE TABLE charge_exploitation (
                                     id UUID PRIMARY KEY,
                                     tenant_id UUID NOT NULL,
                                     libelle VARCHAR(150) NOT NULL,
                                     categorie VARCHAR(50) NOT NULL,
                                     montant NUMERIC(19, 4) NOT NULL,
                                     date DATE NOT NULL,
                                     description VARCHAR(1000),
                                     version BIGINT NOT NULL DEFAULT 0,

                                     CONSTRAINT ck_charge_exploitation_montant_positif
                                         CHECK (montant >= 0)
);

CREATE INDEX idx_charge_exploitation_tenant_date
    ON charge_exploitation (tenant_id, date);