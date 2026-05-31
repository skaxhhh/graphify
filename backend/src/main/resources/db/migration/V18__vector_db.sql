CREATE TABLE vector_index_stats (
    id                 BIGINT PRIMARY KEY DEFAULT 1,
    total_vectors      BIGINT        NOT NULL DEFAULT 0,
    index_size_bytes   BIGINT        NOT NULL DEFAULT 0,
    vectors_by_type    JSONB         NOT NULL DEFAULT '{}',
    avg_latency_ms     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    avg_similarity     NUMERIC(6, 4) NOT NULL DEFAULT 0,
    request_count_24h  BIGINT        NOT NULL DEFAULT 0,
    latency_series     JSONB         NOT NULL DEFAULT '[]',
    similarity_series  JSONB         NOT NULL DEFAULT '[]',
    request_series     JSONB         NOT NULL DEFAULT '[]',
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT vector_index_stats_singleton CHECK (id = 1)
);

CREATE TABLE embedding_jobs (
    id             BIGSERIAL PRIMARY KEY,
    job_type       VARCHAR(32)  NOT NULL,
    scope          VARCHAR(32)  NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    progress       INT          NOT NULL DEFAULT 0,
    message        TEXT,
    target_ids     JSONB,
    deleted_count  BIGINT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_embedding_jobs_created ON embedding_jobs (created_at DESC);

INSERT INTO vector_index_stats (
    id,
    total_vectors,
    index_size_bytes,
    vectors_by_type,
    avg_latency_ms,
    avg_similarity,
    request_count_24h,
    latency_series,
    similarity_series,
    request_series
) VALUES (
    1,
    12840,
    268435456,
    '{"COMPANY":5200,"INSIGHT":4100,"RELATION":3540}'::jsonb,
    142.50,
    0.8720,
    1842,
    '[120,135,128,142,150,138,145,140,148,152,142,139]'::jsonb,
    '[0.85,0.86,0.87,0.88,0.87,0.86,0.87,0.88,0.87,0.88,0.87,0.87]'::jsonb,
    '[120,140,130,150,160,145,155,148,162,170,184,184]'::jsonb
);

INSERT INTO embedding_jobs (job_type, scope, status, progress, message, completed_at)
VALUES
    ('REINDEX', 'ALL', 'SUCCESS', 100, '전체 재임베딩 완료', NOW() - INTERVAL '2 days'),
    ('CLEANUP', 'ALL', 'SUCCESS', 100, '만료 벡터 120건 삭제', NOW() - INTERVAL '5 days');
