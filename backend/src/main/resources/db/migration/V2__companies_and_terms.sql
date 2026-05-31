CREATE TABLE companies (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    ticker      VARCHAR(32),
    industry    VARCHAR(128),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_companies_name ON companies (LOWER(name));
CREATE INDEX idx_companies_ticker ON companies (LOWER(ticker));

INSERT INTO companies (name, ticker, industry) VALUES
    ('삼성전자', '005930', '반도체'),
    ('SK하이닉스', '000660', '반도체'),
    ('LG에너지솔루션', '373220', '2차전지'),
    ('현대자동차', '005380', '자동차'),
    ('NAVER', '035420', '인터넷'),
    ('카카오', '035720', '인터넷'),
    ('셀트리온', '068270', '바이오'),
    ('POSCO홀딩스', '005490', '철강');

CREATE TABLE terms_documents (
    id           BIGSERIAL PRIMARY KEY,
    type         VARCHAR(64) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    version      VARCHAR(32) NOT NULL,
    content      TEXT,
    required     BOOLEAN NOT NULL DEFAULT TRUE,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO terms_documents (type, title, version, content, required) VALUES
    ('SERVICE', '서비스 이용약관', '1.0', 'graphify 서비스 이용약관 본문입니다.', TRUE),
    ('PRIVACY', '개인정보 처리방침', '1.0', 'graphify 개인정보 처리방침 본문입니다.', TRUE);
