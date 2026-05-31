CREATE TABLE company_agent_signals (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    signal_kind     VARCHAR(32) NOT NULL,
    label           VARCHAR(512) NOT NULL,
    rationale       TEXT,
    sources         VARCHAR(512),
    sort_order      INT NOT NULL DEFAULT 0,
    generated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_company_agent_signals_company ON company_agent_signals (company_id, sort_order);
