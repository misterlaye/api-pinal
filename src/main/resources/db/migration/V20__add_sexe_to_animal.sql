-- Ajouter la colonne sexe à la table animal
ALTER TABLE animal ADD COLUMN sexe VARCHAR(20);

-- Mettre à jour avec INCONNU par défaut
UPDATE animal SET sexe = 'INCONNU' WHERE sexe IS NULL;

-- Inférence métier pour FEMELLE :
-- 1. Si l'animal a été mère d'un autre animal
UPDATE animal SET sexe = 'FEMELLE' 
WHERE id IN (SELECT mere_id FROM animal WHERE mere_id IS NOT NULL);

-- 2. Si l'animal a au moins une lactation (module production)
-- Note: V19 est exécuté après V5 (production_schema)
UPDATE animal SET sexe = 'FEMELLE' 
WHERE id IN (SELECT animal_id FROM lactation);

-- Rendre la colonne non nulle
ALTER TABLE animal ALTER COLUMN sexe SET NOT NULL;
