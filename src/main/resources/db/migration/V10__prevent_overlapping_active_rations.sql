ALTER TABLE ration
    ADD CONSTRAINT ex_ration_active_periode
    EXCLUDE USING gist (
    tenant_id WITH =,
    animal_id WITH =,
    daterange(
        date_debut,
        COALESCE(date_fin, 'infinity'::date),
        '[]'
    ) WITH &&
)
WHERE (statut = 'ACTIVE');