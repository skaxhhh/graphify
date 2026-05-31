CREATE TABLE terms_acceptances (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    terms_document_id BIGINT NOT NULL REFERENCES terms_documents (id),
    terms_version     VARCHAR(32) NOT NULL,
    accepted_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_terms_acceptance_user_doc UNIQUE (user_id, terms_document_id)
);

CREATE INDEX idx_terms_acceptances_user_id ON terms_acceptances (user_id);
