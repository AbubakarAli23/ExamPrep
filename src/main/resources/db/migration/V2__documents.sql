CREATE TABLE documents (
    id                    UUID         PRIMARY KEY,
    owner_id              UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    original_filename     VARCHAR(255) NOT NULL,
    content_type          VARCHAR(150) NOT NULL,
    file_size_bytes       BIGINT       NOT NULL,
    storage_key           VARCHAR(255) NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    page_count            INTEGER,
    character_count       INTEGER,
    failure_reason        VARCHAR(1000),
    processing_started_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_documents_storage_key UNIQUE (storage_key)
);

CREATE INDEX ix_documents_owner_created ON documents (owner_id, created_at DESC);
CREATE INDEX ix_documents_status ON documents (status);

CREATE TABLE document_chunks (
    id               UUID         PRIMARY KEY,
    document_id      UUID         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_index      INTEGER      NOT NULL,
    chunk_text       TEXT         NOT NULL,
    source_reference VARCHAR(160) NOT NULL,
    character_count  INTEGER      NOT NULL,
    CONSTRAINT ux_document_chunks_order UNIQUE (document_id, chunk_index)
);

CREATE INDEX ix_document_chunks_document ON document_chunks (document_id, chunk_index);
