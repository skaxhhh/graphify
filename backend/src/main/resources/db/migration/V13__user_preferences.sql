ALTER TABLE users
    ADD COLUMN is_premium BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_preferences (
    user_id        BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    custom_prompt  TEXT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 데모 사용자 Premium + 샘플 프롬프트
UPDATE users SET is_premium = TRUE WHERE email = 'demo@graphify.dev';

INSERT INTO user_preferences (user_id, custom_prompt)
SELECT id,
       '삼성전자·SK하이닉스 관계 분석 시 HBM 공급망과 투자 관계를 우선적으로 요약해 주세요.'
FROM users
WHERE email = 'demo@graphify.dev'
ON CONFLICT (user_id) DO NOTHING;
