CREATE TABLE watchlist_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_id  BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    added_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_watchlist_user_company UNIQUE (user_id, company_id)
);

CREATE INDEX idx_watchlist_user_added ON watchlist_items (user_id, added_at DESC);

-- 데모 사용자 관심 기업 3건
INSERT INTO watchlist_items (user_id, company_id)
SELECT u.id, c.id
FROM users u
CROSS JOIN companies c
WHERE u.email = 'demo@graphify.dev'
  AND c.ticker IN ('005930', '000660', '035420');
