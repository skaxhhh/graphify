ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS external_source VARCHAR(32),
    ADD COLUMN IF NOT EXISTS external_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS sync_status VARCHAR(16) NOT NULL DEFAULT 'FULL',
    ADD COLUMN IF NOT EXISTS detail_synced_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS listed BOOLEAN;

UPDATE companies
SET sync_status = 'FULL'
WHERE sync_status IS NULL OR sync_status = '';

CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_external_source_id
    ON companies (external_source, external_id)
    WHERE external_source IS NOT NULL AND external_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_ticker_unique
    ON companies (ticker)
    WHERE ticker IS NOT NULL AND TRIM(ticker) <> '';
