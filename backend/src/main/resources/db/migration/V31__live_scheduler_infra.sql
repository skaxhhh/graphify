-- V31: Live scheduler infrastructure
-- Creates market_holidays, shedlock, and paper_live_symbols tables
-- Pre-inserts 2026 KRX public holidays

CREATE TABLE IF NOT EXISTS market_holidays (
    id           BIGSERIAL PRIMARY KEY,
    holiday_date DATE        NOT NULL,
    description  VARCHAR(100),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_market_holidays_date UNIQUE (holiday_date)
);

INSERT INTO market_holidays (holiday_date, description) VALUES
    ('2026-01-01', '신정'),
    ('2026-01-28', '설날 연휴'),
    ('2026-01-29', '설날'),
    ('2026-01-30', '설날 연휴'),
    ('2026-03-01', '삼일절'),
    ('2026-05-05', '어린이날'),
    ('2026-05-25', '부처님오신날'),
    ('2026-06-06', '현충일'),
    ('2026-08-15', '광복절'),
    ('2026-09-24', '추석 연휴'),
    ('2026-09-25', '추석'),
    ('2026-09-26', '추석 연휴'),
    ('2026-10-03', '개천절'),
    ('2026-10-09', '한글날'),
    ('2026-12-25', '크리스마스'),
    ('2026-12-31', '연말 휴장')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS paper_live_symbols (
    id         BIGSERIAL PRIMARY KEY,
    rule_id    BIGINT      NOT NULL REFERENCES trading_rules(id) ON DELETE CASCADE,
    symbol     VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_paper_live_symbols UNIQUE (rule_id, symbol)
);

CREATE INDEX IF NOT EXISTS idx_paper_live_symbols_rule ON paper_live_symbols (rule_id);
