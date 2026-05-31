ALTER TABLE company_dart_snapshots
    ADD COLUMN IF NOT EXISTS financials_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS news_json JSONB NOT NULL DEFAULT '[]'::jsonb;
