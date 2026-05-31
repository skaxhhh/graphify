CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    terms_accepted  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_auth_providers (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider          VARCHAR(32) NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_auth_provider UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_user_auth_provider_user UNIQUE (user_id, provider)
);

CREATE INDEX idx_users_email ON users (email);

-- dev 시드 (비밀번호: password123, {noop} — 개발 전용)
INSERT INTO users (email, password_hash, display_name, role, terms_accepted)
VALUES
    ('demo@graphify.dev', '{noop}password123', '데모 사용자', 'USER', TRUE),
    ('newuser@graphify.dev', '{noop}password123', '신규 사용자', 'USER', FALSE);
