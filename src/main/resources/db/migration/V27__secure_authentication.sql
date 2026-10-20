-- =====================================================================
-- Sécurisation de l'authentification
--  * le PIN n'est plus stocké en clair : colonne d'empreinte BCrypt (remplie par V28) ;
--  * verrouillage du PIN après échecs répétés ;
--  * moyen d'authentification porté par les refresh tokens (une session PIN reste une session PIN).
-- =====================================================================

ALTER TABLE utilisateur RENAME COLUMN code_pin TO pin_hash;
ALTER TABLE utilisateur ALTER COLUMN pin_hash TYPE VARCHAR(100);
ALTER TABLE utilisateur ADD COLUMN pin_failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE utilisateur ADD COLUMN pin_locked_until TIMESTAMPTZ;

ALTER TABLE refresh_token ADD COLUMN auth_method VARCHAR(10) NOT NULL DEFAULT 'OTP';
ALTER TABLE refresh_token ADD CONSTRAINT ck_refresh_token_auth_method CHECK (auth_method IN ('OTP', 'PIN'));

-- Le moyen d'authentification des sessions existantes est inconnu : elles sont révoquées.
UPDATE refresh_token SET revoked = TRUE WHERE revoked = FALSE;
