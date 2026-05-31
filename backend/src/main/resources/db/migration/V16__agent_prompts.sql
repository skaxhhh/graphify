CREATE TABLE agent_prompts (
    id              BIGSERIAL PRIMARY KEY,
    task_type       VARCHAR(64)  NOT NULL UNIQUE,
    system_prompt   TEXT         NOT NULL,
    task_template   TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE agent_prompt_versions (
    id              BIGSERIAL PRIMARY KEY,
    prompt_id       BIGINT       NOT NULL REFERENCES agent_prompts (id) ON DELETE CASCADE,
    version_number  INT          NOT NULL,
    system_prompt   TEXT         NOT NULL,
    task_template   TEXT         NOT NULL,
    change_note     VARCHAR(255),
    author_id       BIGINT,
    author_name     VARCHAR(128) NOT NULL DEFAULT '관리자',
    summary         VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_agent_prompt_versions_prompt_version UNIQUE (prompt_id, version_number)
);

CREATE INDEX idx_agent_prompt_versions_prompt_id ON agent_prompt_versions (prompt_id, created_at DESC);

INSERT INTO agent_prompts (task_type, system_prompt, task_template)
VALUES
    (
        'RELATION_ANALYSIS',
        '당신은 기업 관계 분석 전문 AI입니다. 모든 분석 결과는 한국어로 제공하며, 투자 판단의 근거로 활용될 수 있음을 인지하고 객관적 사실 기반으로 분석합니다.',
        '대상 기업의 공급망·투자·협력 관계를 그래프 관점에서 요약하고, 핵심 연결 기업과 관계 유형을 설명하세요.'
    ),
    (
        'RISK_DETECTION',
        '당신은 기업 리스크 탐지 전문 AI입니다. 공시·뉴스·재무 신호를 바탕으로 객관적으로 리스크 요인을 식별합니다.',
        '대상 기업의 단기·중기 리스크 요인을 나열하고, 각 항목에 근거 데이터 유형(공시/뉴스/재무)을 명시하세요.'
    ),
    (
        'INSIGHT_SUMMARY',
        '당신은 투자 인사이트 요약 전문 AI입니다. 분석 결과를 비전문가도 이해할 수 있게 간결히 정리합니다.',
        '분석 결과를 3~5개 불릿 인사이트로 요약하고, 각 인사이트에 신뢰도(높음/중간/낮음)를 표시하세요.'
    );

INSERT INTO agent_prompt_versions (prompt_id, version_number, system_prompt, task_template, change_note, author_name, summary)
SELECT id, 1, system_prompt, task_template, '초기 시드', '시스템', 'v1 초기 버전'
FROM agent_prompts;
