-- =====================================================================
-- Le litre (L) est l'unité de référence du lait, de la saisie aux calculs financiers.
--
-- Les volumes existants sont conservés tels quels : l'écran de saisie par lot les présentait
-- déjà en litres à l'ouvrier. La conversion kg -> L (0,971) appliquée par la finance est supprimée.
-- =====================================================================

ALTER TABLE traite RENAME COLUMN quantite_kg TO quantite_litres;

COMMENT ON COLUMN traite.quantite_litres IS 'Volume de lait de la traite, en litres (unité de référence).';

-- Une seule traite MATIN et une seule traite SOIR par lactation et par jour civil (UTC).
-- Le type AUTRE (traite exceptionnelle) n'est pas limité. Protège contre les doubles envois.
CREATE UNIQUE INDEX uk_traite_lactation_type_jour
    ON traite (lactation_id, type, ((date_heure AT TIME ZONE 'UTC')::date))
    WHERE type <> 'AUTRE';
