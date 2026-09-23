-- =====================================================================
-- V16 : Auth — Tables OTP & Refresh Token (JWT natif)
-- =====================================================================

-- =====================================================================
-- Table otp_code : codes OTP pour l'authentification passwordless
-- =====================================================================
CREATE TABLE otp_code (
    id          UUID PRIMARY KEY,
    telephone   VARCHAR(20)  NOT NULL,
    code_hash   VARCHAR(256) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_otp_code_telephone
    ON otp_code (telephone, used, expires_at);

-- =====================================================================
-- Table refresh_token : tokens de rafraîchissement révocables
-- =====================================================================
CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    token_hash  VARCHAR(256) NOT NULL,
    user_id     UUID         NOT NULL REFERENCES utilisateur(id),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_refresh_token_hash
    ON refresh_token (token_hash);

CREATE INDEX idx_refresh_token_user
    ON refresh_token (user_id, revoked);
