CREATE TABLE analyse_lait
(
    id                  UUID PRIMARY KEY,
    tenant_id           UUID                     NOT NULL,
    exploitation_id     UUID                     NOT NULL,
    lactation_id        UUID                     NOT NULL REFERENCES lactation (id),
    auteur_id           UUID                     NOT NULL,
    date_analyse        DATE                     NOT NULL,
    taux_butyreux       NUMERIC(5, 2),
    taux_proteique      NUMERIC(5, 2),
    cellules_somatiques INTEGER,
    date_heure_saisie   TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT                   NOT NULL DEFAULT 0
);

CREATE INDEX idx_analyse_lait_lactation ON analyse_lait (lactation_id);
CREATE INDEX idx_analyse_lait_exploitation ON analyse_lait (exploitation_id);
