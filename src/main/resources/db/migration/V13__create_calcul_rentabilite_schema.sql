CREATE TABLE calcul_rentabilite (
                                    id UUID PRIMARY KEY,
                                    tenant_id UUID NOT NULL,
                                    date_calcul TIMESTAMPTZ NOT NULL,
                                    periode_debut DATE NOT NULL,
                                    periode_fin DATE NOT NULL,
                                    volume_lait NUMERIC(19, 4) NOT NULL,
                                    chiffre_affaires NUMERIC(19, 4) NOT NULL,
                                    cout_alimentation NUMERIC(19, 4) NOT NULL,
                                    autres_charges NUMERIC(19, 4) NOT NULL,
                                    cout_total NUMERIC(19, 4) NOT NULL,
                                    cout_revient_par_litre NUMERIC(19, 4) NOT NULL,
                                    marge NUMERIC(19, 4) NOT NULL,
                                    version BIGINT NOT NULL DEFAULT 0,

                                    CONSTRAINT ck_calcul_rentabilite_periode
                                        CHECK (periode_fin >= periode_debut),

                                    CONSTRAINT ck_calcul_rentabilite_volume
                                        CHECK (volume_lait > 0),

                                    CONSTRAINT ck_calcul_rentabilite_ca
                                        CHECK (chiffre_affaires >= 0),

                                    CONSTRAINT ck_calcul_rentabilite_cout_alimentation
                                        CHECK (cout_alimentation >= 0),

                                    CONSTRAINT ck_calcul_rentabilite_autres_charges
                                        CHECK (autres_charges >= 0),

                                    CONSTRAINT ck_calcul_rentabilite_cout_total
                                        CHECK (cout_total >= 0),

                                    CONSTRAINT ck_calcul_rentabilite_cout_litre
                                        CHECK (cout_revient_par_litre >= 0)
);

CREATE INDEX idx_calcul_rentabilite_tenant
    ON calcul_rentabilite (tenant_id);

CREATE INDEX idx_calcul_rentabilite_tenant_periode
    ON calcul_rentabilite (
                           tenant_id,
                           periode_debut,
                           periode_fin
        );