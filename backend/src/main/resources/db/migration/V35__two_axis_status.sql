-- V35: 2축 상태 모델 도입 (config_status x run_status)
-- 기존 단일 status 컬럼은 호환성을 위해 유지 (제거하지 않음)
ALTER TABLE trading_rules
    ADD COLUMN IF NOT EXISTS config_status VARCHAR(8) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS run_status    VARCHAR(8) NOT NULL DEFAULT 'STOPPED';

-- 기존 행 마이그레이션
UPDATE trading_rules SET config_status = 'ACTIVE', run_status = 'RUNNING'
    WHERE status = 'PAPER_LIVE';
UPDATE trading_rules SET config_status = 'ACTIVE', run_status = 'STOPPED'
    WHERE status = 'PAUSED';
UPDATE trading_rules SET config_status = 'DRAFT',  run_status = 'STOPPED'
    WHERE status IN ('DRAFT', 'BACKTESTED');

CREATE INDEX IF NOT EXISTS idx_trading_rules_run_status ON trading_rules (run_status);
