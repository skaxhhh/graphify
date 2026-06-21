-- V34: Toss Securities credential storage (encrypted client_id, client_secret, access_token)
CREATE TABLE IF NOT EXISTS toss_credentials (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    client_id_encrypted     TEXT NOT NULL,
    client_secret_encrypted TEXT NOT NULL,
    access_token_encrypted  TEXT,
    token_expires_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_toss_credentials_user_id ON toss_credentials (user_id);
