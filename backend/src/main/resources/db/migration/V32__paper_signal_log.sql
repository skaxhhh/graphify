-- V32: paper_signal_log — per-tick rule evaluation signal log with indicator snapshot
CREATE TABLE IF NOT EXISTS paper_signal_log (
    id                  BIGSERIAL PRIMARY KEY,
    rule_id             BIGINT       NOT NULL REFERENCES trading_rules(id) ON DELETE CASCADE,
    symbol              VARCHAR(32)  NOT NULL,
    ts                  TIMESTAMPTZ  NOT NULL,
    signal              VARCHAR(8)   NOT NULL,   -- BUY | SELL | HOLD
    indicator_snapshot  JSONB,                   -- {"rsi": 68.2, "sma20": 72500.0, "price": 73000.0}
    executed            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_paper_signal_log_rule_ts ON paper_signal_log (rule_id, ts DESC);
CREATE INDEX IF NOT EXISTS idx_paper_signal_log_ts ON paper_signal_log (ts DESC);
