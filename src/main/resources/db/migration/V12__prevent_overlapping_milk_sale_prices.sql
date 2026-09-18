ALTER TABLE prix_vente_lait
    ADD CONSTRAINT ex_prix_vente_lait_periode
    EXCLUDE USING gist (
    tenant_id WITH =,
    daterange(
        date_debut,
        COALESCE(date_fin, 'infinity'::date),
        '[]'
    ) WITH &&
);