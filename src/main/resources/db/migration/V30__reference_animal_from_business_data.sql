-- =====================================================================
-- Intégrité référentielle vers l'animal
--
-- Lactations, rations et événements sanitaires référencent un animal sans contrainte :
-- supprimer un animal laissait ces données orphelines (et toujours comptées dans la production).
-- Les clés étrangères rendent la suppression impossible tant qu'un historique existe
-- (la sortie du troupeau reste la voie normale).
--
-- NOT VALID : la contrainte s'applique à toute nouvelle écriture et à toute suppression d'animal,
-- sans échouer sur d'éventuelles lignes orphelines antérieures.
-- =====================================================================

ALTER TABLE lactation
    ADD CONSTRAINT fk_lactation_animal FOREIGN KEY (animal_id) REFERENCES animal (id) NOT VALID;

ALTER TABLE ration
    ADD CONSTRAINT fk_ration_animal FOREIGN KEY (animal_id) REFERENCES animal (id) NOT VALID;

ALTER TABLE evenement_sanitaire
    ADD CONSTRAINT fk_evenement_sanitaire_animal FOREIGN KEY (animal_id) REFERENCES animal (id) NOT VALID;
