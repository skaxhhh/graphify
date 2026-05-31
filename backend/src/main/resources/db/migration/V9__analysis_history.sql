CREATE TABLE analysis_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_id      UUID,
    company_id      BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    company_name    VARCHAR(255) NOT NULL,
    analyzed_at     TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    summary_line    TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analysis_history_user_analyzed ON analysis_history (user_id, analyzed_at DESC);
CREATE INDEX idx_analysis_history_user_company ON analysis_history (user_id, company_name);

-- 데모 사용자 이력
INSERT INTO analysis_history (user_id, session_id, company_id, company_name, analyzed_at, status, summary_line)
SELECT u.id, gen_random_uuid(), c.id, c.name, NOW() - INTERVAL '1 day', 'COMPLETED',
       'HBM 공급망·AI GPU 고객 관계 인사이트 2건'
FROM users u
CROSS JOIN companies c
WHERE u.email = 'demo@graphify.dev' AND c.ticker = '005930';

INSERT INTO analysis_history (user_id, session_id, company_id, company_name, analyzed_at, status, summary_line)
SELECT u.id, gen_random_uuid(), c.id, c.name, NOW() - INTERVAL '3 days', 'COMPLETED',
       '메모리 파트너십·투자 관계 요약'
FROM users u
CROSS JOIN companies c
WHERE u.email = 'demo@graphify.dev' AND c.ticker = '000660';

INSERT INTO analysis_history (user_id, session_id, company_id, company_name, analyzed_at, status, summary_line)
SELECT u.id, gen_random_uuid(), c.id, c.name, NOW() - INTERVAL '5 days', 'COMPLETED',
       '2차전지 밸류체인 점검 완료'
FROM users u
CROSS JOIN companies c
WHERE u.email = 'demo@graphify.dev' AND c.ticker = '373220';

INSERT INTO analysis_history (user_id, session_id, company_id, company_name, analyzed_at, status, summary_line)
SELECT u.id, gen_random_uuid(), c.id, c.name, NOW() - INTERVAL '7 days', 'FAILED',
       '외부 API 일시 오류로 분석 중단'
FROM users u
CROSS JOIN companies c
WHERE u.email = 'demo@graphify.dev' AND c.ticker = '005380';

INSERT INTO analysis_history (user_id, session_id, company_id, company_name, analyzed_at, status, summary_line)
SELECT u.id, gen_random_uuid(), c.id, c.name, NOW() - INTERVAL '10 days', 'COMPLETED',
       '플랫폼·핀테크 연관 관계 3건'
FROM users u
CROSS JOIN companies c
WHERE u.email = 'demo@graphify.dev' AND c.ticker = '035420';

INSERT INTO analysis_history (user_id, session_id, company_id, company_name, analyzed_at, status, summary_line)
SELECT u.id, gen_random_uuid(), c.id, c.name, NOW() - INTERVAL '14 days', 'COMPLETED',
       '슈퍼앱 생태계 관계망 분석'
FROM users u
CROSS JOIN companies c
WHERE u.email = 'demo@graphify.dev' AND c.ticker = '035720';
