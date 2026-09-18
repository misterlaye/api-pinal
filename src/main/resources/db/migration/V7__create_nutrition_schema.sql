CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE aliment (
                         id UUID PRIMARY KEY,
                         code VARCHAR(50) NOT NULL,
                         nom VARCHAR(150) NOT NULL,
                         categorie VARCHAR(100) NOT NULL,
                         unite VARCHAR(20) NOT NULL,
                         actif BOOLEAN NOT NULL,
                         date_creation TIMESTAMPTZ NOT NULL,
                         date_modification TIMESTAMPTZ NOT NULL,
                         version BIGINT NOT NULL DEFAULT 0,

                         CONSTRAINT uk_aliment_code
                             UNIQUE (code),

                         CONSTRAINT ck_aliment_unite
                             CHECK (unite IN ('KG', 'SAC', 'LITRE', 'AUTRE'))
);

CREATE TABLE prix_aliment (
                              id UUID PRIMARY KEY,
                              aliment_id UUID NOT NULL,
                              prix_unitaire NUMERIC(19,4) NOT NULL,
                              date_debut DATE NOT NULL,
                              date_fin DATE,
                              version BIGINT NOT NULL DEFAULT 0,

                              CONSTRAINT fk_prix_aliment_aliment
                                  FOREIGN KEY (aliment_id)
                                      REFERENCES aliment(id),

                              CONSTRAINT ck_prix_aliment_positif
                                  CHECK (prix_unitaire > 0),

                              CONSTRAINT ck_prix_aliment_dates
                                  CHECK (
                                      date_fin IS NULL
                                          OR date_fin >= date_debut
                                      )
);

ALTER TABLE prix_aliment
    ADD CONSTRAINT ex_prix_aliment_periode
    EXCLUDE USING gist (
    aliment_id WITH =,
    daterange(
        date_debut,
        COALESCE(date_fin, 'infinity'::date),
        '[]'
    ) WITH &&
);