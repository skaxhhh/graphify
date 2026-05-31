ALTER TABLE companies
    ADD COLUMN market VARCHAR(32),
    ADD COLUMN data_status VARCHAR(32) NOT NULL DEFAULT 'FRESH';

UPDATE companies SET market = 'KOSPI' WHERE ticker IN ('005930', '000660', '005380', '035420', '068270', '005490');
UPDATE companies SET market = 'KOSDAQ' WHERE ticker IN ('373220', '035720');

CREATE INDEX idx_companies_industry ON companies (industry);
CREATE INDEX idx_companies_market ON companies (market);
