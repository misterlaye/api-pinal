-- 1. Nettoyer cycle_reproduction
ALTER TABLE cycle_reproduction DROP COLUMN identifiant_taureau;
ALTER TABLE cycle_reproduction DROP COLUMN constat_date;
ALTER TABLE cycle_reproduction DROP COLUMN constat_resultat;
ALTER TABLE cycle_reproduction DROP COLUMN constat_veterinaire;

-- 2. Ajouter les nouveaux champs taureau/paillette
ALTER TABLE cycle_reproduction ADD COLUMN taureau_id UUID REFERENCES animal(id);
ALTER TABLE cycle_reproduction ADD COLUMN code_paillette VARCHAR(100);

-- 3. Créer la table pour l'historique des constats
CREATE TABLE constat_gestation (
    cycle_id UUID NOT NULL REFERENCES cycle_reproduction(id),
    date_constat DATE NOT NULL,
    resultat VARCHAR(20) NOT NULL,
    veterinaire VARCHAR(100)
);

CREATE INDEX idx_constat_gestation_cycle ON constat_gestation(cycle_id);

-- 4. Ajouter le lien pour la naissance des veaux
ALTER TABLE animal ADD COLUMN cycle_reproduction_id UUID REFERENCES cycle_reproduction(id);
