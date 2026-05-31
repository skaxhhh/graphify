CREATE TABLE admin_metrics_daily (
    metric_date       DATE         PRIMARY KEY,
    run_count         INT          NOT NULL DEFAULT 0,
    avg_duration_ms   BIGINT       NOT NULL DEFAULT 0,
    token_usage       BIGINT       NOT NULL DEFAULT 0,
    error_count       INT          NOT NULL DEFAULT 0
);

CREATE TABLE admin_alerts (
    id           BIGSERIAL PRIMARY KEY,
    severity     VARCHAR(16)  NOT NULL,
    message      TEXT         NOT NULL,
    detected_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_alerts_detected ON admin_alerts (detected_at DESC);

-- 관리자 계정 (비밀번호: admin1234)
INSERT INTO users (email, password_hash, display_name, role, terms_accepted, is_premium)
SELECT 'admin@graphify.dev', '{noop}admin1234', '관리자', 'ADMIN', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@graphify.dev');

-- 최근 14일 일별 메트릭 시드
INSERT INTO admin_metrics_daily (metric_date, run_count, avg_duration_ms, token_usage, error_count)
SELECT d::date,
       40 + (EXTRACT(DOY FROM d)::int % 7) * 5,
       12000 + (EXTRACT(DOY FROM d)::int % 5) * 800,
       150000 + (EXTRACT(DOY FROM d)::int % 9) * 12000,
       (EXTRACT(DOY FROM d)::int % 11)
FROM generate_series(CURRENT_DATE - INTERVAL '13 days', CURRENT_DATE, INTERVAL '1 day') AS d;

INSERT INTO admin_alerts (severity, message, detected_at)
VALUES
    ('WARN', '오류율이 24시간 평균 대비 상승했습니다.', NOW() - INTERVAL '2 hours'),
    ('INFO', '토큰 사용량이 주간 목표의 80%에 도달했습니다.', NOW() - INTERVAL '6 hours');
