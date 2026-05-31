CREATE TABLE company_dart_snapshots (
    company_id      BIGINT PRIMARY KEY REFERENCES companies (id) ON DELETE CASCADE,
    corp_code       VARCHAR(8) NOT NULL,
    profile_json    JSONB NOT NULL,
    disclosures_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    collected_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_company_dart_snapshots_collected ON company_dart_snapshots (collected_at DESC);

CREATE TABLE company_agent_insights (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL UNIQUE REFERENCES companies (id) ON DELETE CASCADE,
    task_type       VARCHAR(64) NOT NULL DEFAULT 'INSIGHT_SUMMARY',
    content         TEXT NOT NULL,
    model_label     VARCHAR(128),
    status          VARCHAR(32) NOT NULL DEFAULT 'READY',
    generated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_company_agent_insights_company ON company_agent_insights (company_id);
