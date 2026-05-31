CREATE TABLE mcp_tools (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(128)  NOT NULL UNIQUE,
    description       TEXT,
    endpoint_url      VARCHAR(512)  NOT NULL,
    auth_type         VARCHAR(32)   NOT NULL DEFAULT 'NONE',
    auth_secret       VARCHAR(512),
    schema_json       TEXT,
    connection_status VARCHAR(32)   NOT NULL DEFAULT 'UNKNOWN',
    enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    allowed_roles     VARCHAR(128)  NOT NULL DEFAULT 'USER',
    last_called_at    TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mcp_tools_enabled ON mcp_tools (enabled);
CREATE INDEX idx_mcp_tools_status ON mcp_tools (connection_status);

INSERT INTO mcp_tools (name, description, endpoint_url, auth_type, connection_status, enabled, allowed_roles, last_called_at)
VALUES
    (
        'company-search',
        '기업 검색 및 메타데이터 조회 MCP',
        'http://localhost:8090/mcp/company-search',
        'API_KEY',
        'CONNECTED',
        TRUE,
        'USER,PREMIUM',
        NOW() - INTERVAL '1 hour'
    ),
    (
        'news-ingest',
        '뉴스·공시 수집 MCP',
        'http://localhost:8090/mcp/news-ingest',
        'BEARER',
        'DISCONNECTED',
        TRUE,
        'ADMIN,PREMIUM',
        NOW() - INTERVAL '2 days'
    ),
    (
        'graph-relations',
        '관계 그래프 확장 MCP',
        'http://localhost:8090/mcp/graph-relations',
        'NONE',
        'UNKNOWN',
        FALSE,
        'USER,ADMIN,PREMIUM',
        NULL
    );
