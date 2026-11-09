CREATE TABLE study_packages (
    id                        UUID         PRIMARY KEY,
    owner_id                  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    document_id               UUID         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    title                     VARCHAR(300),
    status                    VARCHAR(32)  NOT NULL,
    progress_message          VARCHAR(300),
    language                  VARCHAR(40),
    academic_level            VARCHAR(60),
    exam_date                 DATE,
    requested_question_count  INTEGER      NOT NULL,
    requested_mock_question_count INTEGER  NOT NULL,
    requested_flashcard_count INTEGER      NOT NULL,
    document_intro            TEXT,
    overall_summary           TEXT,
    learning_objectives       JSONB,
    revision_priorities       JSONB,
    topics                    JSONB,
    revision_notes            JSONB,
    definitions               JSONB,
    formulas                  JSONB,
    failure_reason            VARCHAR(1000),
    ai_model                  VARCHAR(120),
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,
    completed_at              TIMESTAMPTZ,
    CONSTRAINT fk_study_packages_document UNIQUE (id, document_id)
);

CREATE INDEX ix_study_packages_owner_created ON study_packages (owner_id, created_at DESC);
CREATE INDEX ix_study_packages_status ON study_packages (status);

CREATE TABLE flashcards (
    id               UUID         PRIMARY KEY,
    study_package_id UUID         NOT NULL REFERENCES study_packages (id) ON DELETE CASCADE,
    position         INTEGER      NOT NULL,
    front            TEXT         NOT NULL,
    back             TEXT         NOT NULL,
    source_reference VARCHAR(160)
);

CREATE INDEX ix_flashcards_package ON flashcards (study_package_id, position);

CREATE TABLE quiz_questions (
    id               UUID         PRIMARY KEY,
    study_package_id UUID         NOT NULL REFERENCES study_packages (id) ON DELETE CASCADE,
    section          VARCHAR(20)  NOT NULL,
    position         INTEGER      NOT NULL,
    type             VARCHAR(24)  NOT NULL,
    prompt           TEXT         NOT NULL,
    options          JSONB,
    correct_answer   TEXT         NOT NULL,
    explanation      TEXT,
    difficulty       VARCHAR(20)  NOT NULL,
    source_reference VARCHAR(160)
);

CREATE INDEX ix_quiz_questions_package ON quiz_questions (study_package_id, section, position);

CREATE TABLE quiz_attempts (
    id                 UUID         PRIMARY KEY,
    owner_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    study_package_id   UUID         NOT NULL REFERENCES study_packages (id) ON DELETE CASCADE,
    question_section   VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    total_questions    INTEGER      NOT NULL,
    gradable_questions INTEGER      NOT NULL,
    allowed_seconds    INTEGER      NOT NULL,
    correct_count      INTEGER,
    score_percent      INTEGER,
    duration_seconds   INTEGER,
    started_at         TIMESTAMPTZ  NOT NULL,
    submitted_at       TIMESTAMPTZ,
    answers            JSONB
);

CREATE INDEX ix_quiz_attempts_owner ON quiz_attempts (owner_id, submitted_at DESC);
CREATE INDEX ix_quiz_attempts_status ON quiz_attempts (status);

CREATE TABLE flashcard_reviews (
    id               UUID         PRIMARY KEY,
    owner_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    study_package_id UUID         NOT NULL REFERENCES study_packages (id) ON DELETE CASCADE,
    reviewed_count   INTEGER      NOT NULL,
    reviewed_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_flashcard_reviews_owner ON flashcard_reviews (owner_id, reviewed_at DESC);

CREATE TABLE tutor_messages (
    id                 UUID         PRIMARY KEY,
    owner_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    study_package_id   UUID         NOT NULL REFERENCES study_packages (id) ON DELETE CASCADE,
    role               VARCHAR(16)  NOT NULL,
    content            TEXT         NOT NULL,
    source_references  JSONB,
    created_at         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_tutor_messages_package ON tutor_messages (study_package_id, created_at);
