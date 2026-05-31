CREATE TABLE relationship_nodes (
    company_id    BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    node_id       VARCHAR(32)  NOT NULL,
    label         VARCHAR(255) NOT NULL,
    node_type     VARCHAR(32)  NOT NULL DEFAULT 'COMPANY',
    summary       TEXT,
    degree        INT          NOT NULL DEFAULT 1,
    cluster_id    VARCHAR(32),
    depth_level   INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (company_id, node_id)
);

CREATE TABLE relationship_edges (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    source_node_id  VARCHAR(32)  NOT NULL,
    target_node_id  VARCHAR(32)  NOT NULL,
    relation_type   VARCHAR(32)  NOT NULL,
    strength        DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    evidence        TEXT,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_edge_source FOREIGN KEY (company_id, source_node_id)
        REFERENCES relationship_nodes (company_id, node_id) ON DELETE CASCADE,
    CONSTRAINT fk_edge_target FOREIGN KEY (company_id, target_node_id)
        REFERENCES relationship_nodes (company_id, node_id) ON DELETE CASCADE
);

CREATE INDEX idx_relationship_nodes_company ON relationship_nodes (company_id, depth_level);
CREATE INDEX idx_relationship_edges_company ON relationship_edges (company_id, relation_type);

CREATE TABLE agent_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  BIGINT NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    status      VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 삼성전자 (insight highlight id와 정합)
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, cluster_id, depth_level)
SELECT c.id, '1', c.name, 'COMPANY', '그래프 중심 기업', 0, NULL, 0 FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, cluster_id, depth_level)
SELECT c.id, '2', 'SK하이닉스', 'COMPANY', '메모리 공급망 파트너', 1, NULL, 1 FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, cluster_id, depth_level)
SELECT c.id, '3', '반도체 장비사', 'COMPANY', 'CapEx 연관 장비 공급', 1, 'cluster-equip', 1 FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, cluster_id, depth_level)
SELECT c.id, '4', '소재 파트너', 'COMPANY', '첨단 공정 소재 협력', 2, 'cluster-equip', 2 FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, cluster_id, depth_level)
SELECT c.id, '5', 'AI GPU 고객', 'COMPANY', 'HBM 수요 연관', 1, NULL, 1 FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, cluster_id, depth_level)
SELECT c.id, '6', '경쟁 메모리사', 'COMPANY', '시장 점유 경쟁', 2, NULL, 2 FROM companies c WHERE c.ticker = '005930';

INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '2', 'SUPPLY_CHAIN', 0.85, 'HBM·DRAM 공급망 연계' FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '3', 'INVESTMENT', 0.72, '장비 투자·CapEx 확대' FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '3', '4', 'PARTNERSHIP', 0.65, '장비-소재 협력' FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '5', 'SUPPLY_CHAIN', 0.9, 'AI 서버 HBM 수요' FROM companies c WHERE c.ticker = '005930';
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '6', 'RISK', 0.55, '메모리 가격·점유율 경쟁' FROM companies c WHERE c.ticker = '005930';

-- SK하이닉스
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, depth_level)
SELECT c.id, '1', c.name, 'COMPANY', '중심', 0, 0 FROM companies c WHERE c.ticker = '000660';
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, depth_level)
SELECT c.id, '2', '삼성전자', 'COMPANY', '경쟁·협력', 1, 1 FROM companies c WHERE c.ticker = '000660';
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, depth_level)
SELECT c.id, '3', 'AI 고객사', 'COMPANY', 'HBM 수요', 1, 1 FROM companies c WHERE c.ticker = '000660';
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '2', 'PARTNERSHIP', 0.7, '메모리 시장' FROM companies c WHERE c.ticker = '000660';
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '3', 'SUPPLY_CHAIN', 0.88, 'HBM 공급' FROM companies c WHERE c.ticker = '000660';

-- 나머지 기업 공통 4노드 그래프
INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, depth_level)
SELECT c.id, '1', c.name, 'COMPANY', '중심 기업', 0, 0
FROM companies c WHERE c.ticker NOT IN ('005930', '000660');

INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, depth_level)
SELECT c.id, '2', '주요 고객', 'COMPANY', '매출 연관', 1, 1
FROM companies c WHERE c.ticker NOT IN ('005930', '000660');

INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, depth_level)
SELECT c.id, '3', '협력사', 'COMPANY', '공급·제휴', 1, 1
FROM companies c WHERE c.ticker NOT IN ('005930', '000660');

INSERT INTO relationship_nodes (company_id, node_id, label, node_type, summary, degree, depth_level)
SELECT c.id, '4', '투자 대상', 'COMPANY', '지분·M&A', 2, 2
FROM companies c WHERE c.ticker NOT IN ('005930', '000660');

INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '2', 'SUPPLY_CHAIN', 0.75, '매출 연관' FROM companies c WHERE c.ticker NOT IN ('005930', '000660');
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '3', 'PARTNERSHIP', 0.68, '협력 관계' FROM companies c WHERE c.ticker NOT IN ('005930', '000660');
INSERT INTO relationship_edges (company_id, source_node_id, target_node_id, relation_type, strength, evidence)
SELECT c.id, '1', '4', 'INVESTMENT', 0.6, '투자 관계' FROM companies c WHERE c.ticker NOT IN ('005930', '000660');
