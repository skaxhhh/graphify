CREATE TABLE graph_snapshots (
    session_id    UUID         PRIMARY KEY,
    company_id    BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    nodes_json    JSONB        NOT NULL,
    edges_json    JSONB        NOT NULL,
    captured_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE timeline_events (
    id            BIGSERIAL    PRIMARY KEY,
    session_id    UUID         NOT NULL,
    event_at      TIMESTAMPTZ  NOT NULL,
    event_type    VARCHAR(32)  NOT NULL,
    label         VARCHAR(255) NOT NULL,
    payload_json  JSONB,
    sort_order    INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_timeline_events_session ON timeline_events (session_id, sort_order);

CREATE TABLE history_insight_snapshots (
    id                    BIGSERIAL PRIMARY KEY,
    session_id            UUID         NOT NULL,
    card_type             VARCHAR(32)  NOT NULL,
    title                 VARCHAR(255) NOT NULL,
    summary               TEXT         NOT NULL,
    confidence            VARCHAR(16)  NOT NULL,
    evidence              TEXT,
    highlight_node_ids    VARCHAR(255),
    sort_order            INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_history_insight_session ON history_insight_snapshots (session_id, sort_order);

CREATE TABLE history_signal_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          UUID         NOT NULL,
    label               VARCHAR(255) NOT NULL,
    signal_kind         VARCHAR(32)  NOT NULL,
    related_node_ids    VARCHAR(255),
    sources             VARCHAR(512),
    sort_order          INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_history_signal_session ON history_signal_snapshots (session_id, sort_order);

CREATE TABLE history_diff_summaries (
    session_id      UUID         PRIMARY KEY,
    summary_text    TEXT         NOT NULL,
    generated_at    TIMESTAMPTZ  NOT NULL
);

-- 완료된 이력: 그래프 스냅샷 (관계망 depth<=2)
INSERT INTO graph_snapshots (session_id, company_id, nodes_json, edges_json, captured_at)
SELECT
    ah.session_id,
    ah.company_id,
    COALESCE((
        SELECT jsonb_agg(
            jsonb_build_object(
                'id', rn.node_id,
                'label', rn.label,
                'type', rn.node_type,
                'summary', rn.summary,
                'degree', rn.degree,
                'clusterId', rn.cluster_id
            ) ORDER BY rn.depth_level, rn.node_id
        )
        FROM relationship_nodes rn
        WHERE rn.company_id = ah.company_id AND rn.depth_level <= 2
    ), '[]'::jsonb),
    COALESCE((
        SELECT jsonb_agg(
            jsonb_build_object(
                'id', re.id,
                'source', re.source_node_id,
                'target', re.target_node_id,
                'relationType', re.relation_type,
                'strength', re.strength,
                'evidence', re.evidence,
                'updatedAt', re.updated_at
            )
        )
        FROM relationship_edges re
        WHERE re.company_id = ah.company_id
    ), '[]'::jsonb),
    ah.analyzed_at
FROM analysis_history ah
WHERE ah.status = 'COMPLETED' AND ah.session_id IS NOT NULL;

-- 타임라인 이벤트
INSERT INTO timeline_events (session_id, event_at, event_type, label, payload_json, sort_order)
SELECT ah.session_id, ah.analyzed_at - INTERVAL '2 hours', 'ANALYSIS_START', '분석 시작',
       '{"stage":"init"}'::jsonb, 0
FROM analysis_history ah
WHERE ah.status = 'COMPLETED' AND ah.session_id IS NOT NULL;

INSERT INTO timeline_events (session_id, event_at, event_type, label, payload_json, sort_order)
SELECT ah.session_id, ah.analyzed_at - INTERVAL '1 hour', 'GRAPH_BUILT', '관계망 구성',
       '{"stage":"graph"}'::jsonb, 1
FROM analysis_history ah
WHERE ah.status = 'COMPLETED' AND ah.session_id IS NOT NULL;

INSERT INTO timeline_events (session_id, event_at, event_type, label, payload_json, sort_order)
SELECT ah.session_id, ah.analyzed_at, 'COMPLETED', '분석 완료', '{}'::jsonb, 2
FROM analysis_history ah
WHERE ah.status = 'COMPLETED' AND ah.session_id IS NOT NULL;

-- 인사이트·시그널 스냅샷
INSERT INTO history_insight_snapshots (
    session_id, card_type, title, summary, confidence, evidence, highlight_node_ids, sort_order
)
SELECT ah.session_id, cic.card_type, cic.title, cic.summary, cic.confidence,
       cic.evidence, cic.highlight_node_ids, cic.sort_order
FROM analysis_history ah
JOIN company_insight_cards cic ON cic.company_id = ah.company_id
WHERE ah.status = 'COMPLETED' AND ah.session_id IS NOT NULL;

INSERT INTO history_signal_snapshots (
    session_id, label, signal_kind, related_node_ids, sources, sort_order
)
SELECT ah.session_id, cs.label, cs.signal_kind, cs.related_node_ids, cs.sources, cs.sort_order
FROM analysis_history ah
JOIN company_signals cs ON cs.company_id = ah.company_id
WHERE ah.status = 'COMPLETED' AND ah.session_id IS NOT NULL;

-- AI 트렌드 요약
INSERT INTO history_diff_summaries (session_id, summary_text, generated_at)
SELECT ah.session_id,
       '분석 시점 기준 ' || ah.company_name
           || '의 공급망·투자 관계에서 핵심 파트너 노드 비중이 유지되었습니다. '
           || '현재 그래프와 비교 시 리스크·기회 신호 변화를 확인할 수 있습니다.',
       ah.analyzed_at + INTERVAL '5 minutes'
FROM analysis_history ah
WHERE ah.status = 'COMPLETED' AND ah.session_id IS NOT NULL;
