ALTER TABLE companies
    ADD COLUMN summary TEXT;

UPDATE companies SET summary = CASE name
    WHEN '삼성전자' THEN '메모리·파운드리·모바일 DX를 아우르는 글로벌 반도체·전자 기업입니다.'
    WHEN 'SK하이닉스' THEN 'DRAM·NAND 메모리 반도체 중심의 글로벌 메모리 기업입니다.'
    WHEN 'LG에너지솔루션' THEN '전기차 배터리 및 에너지 저장 솔루션을 제공하는 2차전지 기업입니다.'
    WHEN '현대자동차' THEN '완성차·모빌리티·수소·전동화를 선도하는 글로벌 자동차 제조사입니다.'
    WHEN 'NAVER' THEN '검색·커머스·핀테크·클라우드를 운영하는 국내 대표 인터넷 플랫폼 기업입니다.'
    WHEN '카카오' THEN '메신저·모빌리티·금융·콘텐츠 기반의 슈퍼앱 생태계를 구축한 인터넷 기업입니다.'
    WHEN '셀트리온' THEN '바이오시밀러·신약 R&D를 중심으로 하는 글로벌 바이오 제약 기업입니다.'
    WHEN 'POSCO홀딩스' THEN '철강·친환경 미래소재 사업을 영위하는 종합 소재 기업입니다.'
    ELSE summary
END;

CREATE TABLE company_insight_cards (
    id                  BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    card_type           VARCHAR(32) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    summary             TEXT NOT NULL,
    confidence          VARCHAR(16) NOT NULL,
    evidence            TEXT,
    highlight_node_ids  VARCHAR(255),
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE company_signals (
    id                  BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    label               VARCHAR(255) NOT NULL,
    signal_kind         VARCHAR(32) NOT NULL,
    related_node_ids    VARCHAR(255),
    sources             VARCHAR(512),
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_company_insight_cards_company ON company_insight_cards (company_id, sort_order);
CREATE INDEX idx_company_signals_company ON company_signals (company_id, sort_order);

-- 시드: 삼성전자
INSERT INTO company_insight_cards (company_id, card_type, title, summary, confidence, evidence, highlight_node_ids, sort_order)
SELECT id, 'SUPPLY_CHAIN', 'HBM 수요 확대 수혜', 'AI 서버 수요 증가로 HBM 공급망 관련주 동반 모멘텀이 관측됩니다.', 'HIGH',
       '최근 분기 공시 및 주요 고객사 CAPEX 가이던스를 근거로 Agent가 교차 검증했습니다.', '1,2,5', 1
FROM companies WHERE ticker = '005930';

INSERT INTO company_insight_cards (company_id, card_type, title, summary, confidence, evidence, highlight_node_ids, sort_order)
SELECT id, 'INVESTMENT', '파운드리 투자 확대', '첨단 공정 투자가 지속되며 장비·소재 파트너사와의 관계가 강화되고 있습니다.', 'MEDIUM',
       'IR 자료와 업계 리포트에서 CapEx 전망이 상향 조정되었습니다.', '3,4', 2
FROM companies WHERE ticker = '005930';

INSERT INTO company_signals (company_id, label, signal_kind, related_node_ids, sources, sort_order)
SELECT id, 'AI 메모리 수요 둔화 시 마진 압박', 'RISK', '2,6', 'MCP:news-api, MCP:disclosure-api', 1
FROM companies WHERE ticker = '005930';

INSERT INTO company_signals (company_id, label, signal_kind, related_node_ids, sources, sort_order)
SELECT id, 'HBM 점유율 확대 기회', 'OPPORTUNITY', '1,5', 'MCP:disclosure-api', 2
FROM companies WHERE ticker = '005930';

-- SK하이닉스
INSERT INTO company_insight_cards (company_id, card_type, title, summary, confidence, evidence, highlight_node_ids, sort_order)
SELECT id, 'PARTNERSHIP', 'AI 반도체 공급망 핵심 파트너', 'GPU·서버 고객사와의 HBM 공급 관계가 심화되고 있습니다.', 'HIGH',
       '공급 계약 관련 뉴스와 공시 항목을 교차 확인했습니다.', '1,2', 1
FROM companies WHERE ticker = '000660';

INSERT INTO company_signals (company_id, label, signal_kind, related_node_ids, sources, sort_order)
SELECT id, '메모리 가격 사이클 변동성', 'RISK', '3', 'MCP:finance-api', 1
FROM companies WHERE ticker = '000660';

-- 나머지 기업 공통 카드 1개
INSERT INTO company_insight_cards (company_id, card_type, title, summary, confidence, evidence, highlight_node_ids, sort_order)
SELECT c.id, 'SUPPLY_CHAIN', c.industry || ' 밸류체인 점검', c.name || '을 중심으로 업종 내 주요 협력·투자 관계를 요약했습니다.', 'MEDIUM',
       '공개 데이터 기반 1차 관계망 분석 결과입니다.', '1', 1
FROM companies c
WHERE c.ticker NOT IN ('005930', '000660');
