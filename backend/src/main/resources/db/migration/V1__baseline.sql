-- graphify baseline (T01_BOOT)
-- Flyway가 _flyway_schema_history를 관리합니다.

CREATE TABLE IF NOT EXISTS app_metadata (
    id          BIGSERIAL PRIMARY KEY,
    key         VARCHAR(128) NOT NULL UNIQUE,
    value       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE app_metadata IS '애플리케이션 메타·부트스트랩 확인용';
