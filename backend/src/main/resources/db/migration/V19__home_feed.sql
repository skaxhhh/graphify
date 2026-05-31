CREATE TABLE company_view_stats (
    company_id  BIGINT PRIMARY KEY REFERENCES companies (id) ON DELETE CASCADE,
    view_count  BIGINT       NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE market_news (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(512)  NOT NULL,
    summary       TEXT          NOT NULL,
    source_name   VARCHAR(128)  NOT NULL,
    source_url    VARCHAR(1024),
    ticker        VARCHAR(32),
    company_name  VARCHAR(255),
    published_at  TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_market_news_published ON market_news (published_at DESC);

INSERT INTO company_view_stats (company_id, view_count, updated_at)
SELECT c.id,
       CASE c.ticker
           WHEN '005930' THEN 4820
           WHEN '000660' THEN 3910
           WHEN '035420' THEN 2840
           WHEN '005380' THEN 2210
           WHEN '373220' THEN 1980
           WHEN '035720' THEN 1760
           WHEN '068270' THEN 1540
           WHEN '005490' THEN 1320
           ELSE 800
       END,
       NOW() - (c.id * INTERVAL '1 hour')
FROM companies c;

INSERT INTO market_news (title, summary, source_name, source_url, ticker, company_name, published_at)
VALUES
    (
        '삼성전자, HBM3E 양산 확대… AI 서버 수요 견인',
        '메모리 사업부가 분기 캐파를 상향 조정하며 파운드리·HBM 투자 일정을 공유했습니다.',
        '한국경제',
        'https://example.com/news/samsung-hbm',
        '005930',
        '삼성전자',
        NOW() - INTERVAL '35 minutes'
    ),
    (
        'SK하이닉스, 엔비디아 공급망 점검… 장기 계약 논의',
        '고대역폭 메모리 납품 일정과 가격 협상이 시장 관심을 받고 있습니다.',
        '매일경제',
        'https://example.com/news/sk-hynix-nvidia',
        '000660',
        'SK하이닉스',
        NOW() - INTERVAL '1 hour 20 minutes'
    ),
    (
        '현대차, 전기차 북미 판매 목표 상향 조정',
        'IRA 혜택과 현지 생산 비중 확대가 실적 가이던스에 반영될 전망입니다.',
        '연합뉴스',
        'https://example.com/news/hyundai-ev',
        '005380',
        '현대자동차',
        NOW() - INTERVAL '2 hours'
    ),
    (
        'NAVER, 생성형 AI 검색·클라우드 투자 확대',
        '하이퍼클로바X 기반 B2B 매출 성장세가 주가 모멘텀으로 거론됩니다.',
        '조선비즈',
        'https://example.com/news/naver-ai',
        '035420',
        'NAVER',
        NOW() - INTERVAL '3 hours 15 minutes'
    ),
    (
        'LG에너지솔루션, 북미 배터리 JV 가동률 개선',
        '원재료 가격 안정화와 수주 잔고가 2분기 마진 회복 신호로 분석됩니다.',
        '이데일리',
        'https://example.com/news/lg-energy',
        '373220',
        'LG에너지솔루션',
        NOW() - INTERVAL '4 hours'
    ),
    (
        '카카오, 플랫폼 규제·지주사 구조 개편 논의',
        '모빌리티·뱅크 자회사 지분 정리 방안이 투자자 설명회 안건으로 올랐습니다.',
        '서울경제',
        'https://example.com/news/kakao-holding',
        '035720',
        '카카오',
        NOW() - INTERVAL '5 hours 30 minutes'
    ),
    (
        '셀트리온, 바이오시밀러 미국 특허 분쟁 일부 해소',
        '램시마·허쥬마 라인업의 로열티 수익 가시성이 높아졌다는 평가입니다.',
        '머니투데이',
        'https://example.com/news/celltrion-patent',
        '068270',
        '셀트리온',
        NOW() - INTERVAL '7 hours'
    ),
    (
        'POSCO홀딩스, 2차전지 소재·제철 통합 시너지 강조',
        '리튬·니켈 장기 계약과 그린철강 투자 계획이 컨퍼런스콜에서 공유됐습니다.',
        '아시아경제',
        'https://example.com/news/posco-battery',
        '005490',
        'POSCO홀딩스',
        NOW() - INTERVAL '9 hours'
    ),
    (
        '코스피, 반도체·2차전지 동반 상승… 외국인 순매수 전환',
        '장 초반 기관·개인 매수세가 유입되며 대형주 중심으로 지수가 올랐습니다.',
        '한국경제',
        'https://example.com/news/kospi-rally',
        NULL,
        NULL,
        NOW() - INTERVAL '11 hours'
    ),
    (
        '금융위, 기업 공시 AI 검증 파일럿 확대',
        '상장사 실적·관계 공시의 자동 교차검증 시범이 내년 상반기 본격화됩니다.',
        '매일경제',
        'https://example.com/news/fsc-disclosure-ai',
        NULL,
        NULL,
        NOW() - INTERVAL '13 hours'
    );
