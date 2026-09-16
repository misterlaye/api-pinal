CREATE TABLE ration (
                        id UUID PRIMARY KEY,
                        animal_id UUID NOT NULL,
                        date_debut DATE NOT NULL,
                        date_fin DATE,
                        statut VARCHAR(20) NOT NULL,
                        origine VARCHAR(20) NOT NULL,
                        version BIGINT NOT NULL DEFAULT 0,

                        CONSTRAINT ck_ration_statut
                            CHECK (statut IN ('BROUILLON', 'ACTIVE', 'TERMINEE')),

                        CONSTRAINT ck_ration_origine
                            CHECK (origine IN ('ACTUELLE', 'RECOMMANDEE')),

                        CONSTRAINT ck_ration_dates
                            CHECK (
                                date_fin IS NULL
                                    OR date_fin >= date_debut
                                )
);

CREATE TABLE ligne_ration (
                              id UUID PRIMARY KEY,
                              ration_id UUID NOT NULL,
                              aliment_id UUID NOT NULL,
                              quantite NUMERIC(19,4) NOT NULL,

                              CONSTRAINT fk_ligne_ration_ration
                                  FOREIGN KEY (ration_id)
                                      REFERENCES ration(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_ligne_ration_aliment
                                  FOREIGN KEY (aliment_id)
                                      REFERENCES aliment(id),

                              CONSTRAINT ck_ligne_ration_quantite
                                  CHECK (quantite > 0),

                              CONSTRAINT uk_ligne_ration_aliment
                                  UNIQUE (ration_id, aliment_id)
);