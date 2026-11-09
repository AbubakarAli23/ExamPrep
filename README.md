# ExamPrep

ExamPrep is a browser-based examination preparation platform. A student uploads their own course
material — lecture notes, handouts, textbooks or slide decks — and ExamPrep turns the extracted text
into a structured study package: an overview, topics, revision notes, a glossary, formulas, flashcards,
practice questions, a timed mock examination and a document-grounded AI tutor.

Everything is built on Java 21 and Spring Boot 3.5, rendered with Thymeleaf, stored in PostgreSQL and
generated with Google's Gemini API through the official Gen AI Java SDK. There is no JavaScript
framework, no separate frontend build and no microservices.

## Contents

- [Features](#features)
- [Technology stack](#technology-stack)
- [Architecture at a glance](#architecture-at-a-glance)
- [Quick start with Docker Compose](#quick-start-with-docker-compose)
- [Local development without Docker](#local-development-without-docker)
- [Configuration](#configuration)
- [Configuring Gemini](#configuring-gemini)
- [Running the tests](#running-the-tests)
- [Supported documents](#supported-documents)
- [Security and privacy](#security-and-privacy)
- [Database migrations](#database-migrations)
- [Project layout](#project-layout)
- [Manual verification checklist](#manual-verification-checklist)
- [Known limitations](#known-limitations)
- [Troubleshooting](#troubleshooting)

## Features

**Accounts and sessions**
- Registration with Bean Validation, BCrypt password hashing, email/password login, logout.
- Session-based Spring Security with CSRF protection and session fixation migration.
- Every document, study package, attempt and tutor conversation is scoped to its owner. Changing an
  ID in a URL returns 404 rather than another student's data.

**Document ingestion**
- Upload with drag-and-drop or a file picker, progress feedback and a 25 MB configurable limit.
- Extension allow-list plus real content detection with Apache Tika; the browser-supplied MIME type and
  filename are never trusted.
- Files are stored privately under generated UUID names, outside the web root, and are never served by
  URL.
- Asynchronous extraction with persisted states (`UPLOADED`, `PROCESSING`, `READY`, `FAILED`) and browser
  polling.
- Page-aware PDF extraction (PDFBox), slide-aware PPTX extraction (POI), Tika for DOCX/DOC/PPT/TXT/
  Markdown/RTF/ODT/ODP.
- Chunks carry real page, slide or section references. Scanned PDFs are detected and explained; OCR is
  not performed.
- Deleting a document removes the stored file, its chunks and its study packages.

**Study packages**
- Generation runs asynchronously with persisted states (`QUEUED`, `PROCESSING`, `READY`, `FAILED`).
- Large documents are processed hierarchically: bounded batches, per-section summaries, then a synthesis
  pass, rather than one oversized request.
- Structured JSON output is deserialised into typed records and validated before it is stored. Invalid or
  incomplete output fails the package with a clear message; no placeholder content is invented.
- Source references returned by the model are validated against the real chunk list. Fabricated
  references are discarded.
- Overview, topics, revision notes, definitions, formulas, flashcards, practice questions, mock
  examination and tutor in one consistent workspace.

**Practice and assessment**
- Flashcards with reveal, previous and next controls.
- Multiple-choice, true/false and short-answer practice questions with explanations and suggested answers.
- Timed mock examinations with question navigation, a countdown, submission confirmation and automatic
  scoring for multiple-choice and true/false answers.
- Correct answers are never rendered into the exam page; scoring happens on the server. Resubmitting a
  submitted attempt cannot change its score.
- Results page shows the score, per-question feedback, correct answers and explanations, and the areas to
  revise.
- Dashboard shows recent documents, study packages, mock exam scores and flashcard activity.

**Tutor**
- Each study package has its own conversation, persisted per student.
- The tutor retrieves the most relevant source chunks and sends them as the primary context.
- Answers cite only real page/slide/section references; the model is instructed to say when the material
  is insufficient rather than invent content.

## Technology stack

| Concern | Technology |
| --- | --- |
| Language | Java 21 |
| Backend | Spring Boot 3.5.16 (Spring MVC, Spring Data JPA, Spring Security) |
| Architecture | Modular monolith |
| Views | Thymeleaf + `thymeleaf-extras-springsecurity6` |
| Styling / interaction | Hand-written CSS and vanilla JavaScript (no framework, no bundler) |
| Database | PostgreSQL, schema owned by Flyway, Hibernate `ddl-auto: validate` |
| Migrations | Flyway (`V1__users.sql`, `V2__documents.sql`, `V3__study_and_ai.sql`) |
| AI | Google Gen AI Java SDK `com.google.genai:google-genai:1.80.0`, Gemini Developer API |
| Extraction | Apache Tika 3.3.3, Apache PDFBox 3.0.8, Apache POI 5.4.1 |
| Build | Maven (wrapper included) |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, embedded PostgreSQL (Zonky) |
| Deployment | Docker multi-stage image (non-root), Docker Compose |

## Architecture at a glance

```
Browser (Thymeleaf pages + plain JS)
        │  HTTP, session cookie, CSRF token
        ▼
web/controller  ──────────────►  application services
                                   ├── user/      accounts, BCrypt, registration
                                   ├── document/  validation, storage, extraction, chunking
                                   ├── ai/        Gemini gateway, prompts, retrieval, validation
                                   └── study/     packages, flashcards, quizzes, attempts, tutor
                                            │
                                            ▼
                                   PostgreSQL (Flyway-managed, JSONB for generated content)
```

- Controllers stay thin: they validate form input, delegate to a service and choose a view.
- Business logic lives in service classes with constructor injection.
- UUID scalar foreign keys are used instead of JPA associations, so queries are explicit and lazy-loading
  issues cannot leak across request boundaries.
- AI access sits behind the `GeminiClient` gateway; business logic never touches SDK classes directly.
- Long-running work runs on bounded `ThreadPoolTaskExecutor`s (`documentTaskExecutor`,
  `studyTaskExecutor`) and communicates state through the database. The browser polls status endpoints.
- `StaleJobRecovery` runs at startup: jobs left `PROCESSING` by a crash are failed with a safe message and
  uploads stuck at `UPLOADED` are retried, so no student is left with a permanently spinning job.

More detail: [docs/architecture.md](docs/architecture.md).

## Quick start with Docker Compose

Requirements: Docker Engine with the Compose plugin.

```bash
git clone <your-repository-url> examprep
cd examprep

cp .env.example .env
# Edit .env: set GOOGLE_API_KEY and change SPRING_DATASOURCE_PASSWORD.

docker compose up --build
```

Open <http://localhost:8080>. The application container waits for PostgreSQL to become healthy, Flyway
applies the migrations on startup, and both the database and uploaded documents live in named volumes
(`examprep-db`, `examprep-uploads`) so they survive `docker compose down` and rebuilds.

The application starts even when `GOOGLE_API_KEY` is empty. Uploading and extraction work; when a study
package or tutor request needs Gemini, the application shows a clear server-side configuration error
instead of fabricating output.

To stop: `docker compose down`. To remove the database and uploads as well:
`docker compose down -v` (destructive).

## Local development without Docker

Requirements: JDK 21 and a PostgreSQL 14+ instance (the Maven wrapper is included).

1. **Start PostgreSQL** and create a database and user:

   ```sql
   CREATE USER examprep WITH PASSWORD 'change-me';
   CREATE DATABASE examprep OWNER examprep;
   ```

   Or use the helper (requires Docker only for the database):
   `scripts/dev-db.sh`

2. **Configure the environment.** Spring Boot does not read `.env` by itself; either export the variables
   or load the file:

   ```bash
   cp .env.example .env
   # edit .env
   set -a; . ./.env; set +a
   ```

3. **Run the application** (Flyway applies migrations automatically):

   ```bash
   ./mvnw spring-boot:run
   ```

4. Open <http://localhost:8080>, register an account and upload a document.

To build the executable jar instead:

```bash
./mvnw clean package
java -jar target/examprep-1.0.0.jar
```

## Configuration

All configuration is environment-driven; nothing secret is committed or compiled into the image.

| Variable | Default | Purpose |
| --- | --- | --- |
| `GOOGLE_API_KEY` | *(empty)* | Gemini Developer API key. Required for study generation and the tutor. |
| `GEMINI_MODEL` | `gemini-3.8-flash` | Gemini model name. Never hardcoded in application code. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/examprep` | JDBC URL. |
| `SPRING_DATASOURCE_USERNAME` | `examprep` | Database user. |
| `SPRING_DATASOURCE_PASSWORD` | `examprep` | Database password. Set a real value outside development. |
| `APP_STORAGE_LOCATION` | `./data/uploads` | Private upload directory. Must not be inside `src/main/resources/static`. |
| `APP_MAX_UPLOAD_SIZE_MB` | `25` | Upload limit, applied to Tomcat multipart and application validation. |
| `SERVER_PORT` | `8080` | HTTP port. |
| `GEMINI_REQUEST_TIMEOUT_SECONDS` | `150` | Per-request timeout for Gemini calls. |
| `GEMINI_MAX_RETRIES` | `2` | Retries for transient failures (429 and 5xx). |
| `GEMINI_MAX_OUTPUT_TOKENS` | `16384` | Output ceiling for a generation request. |
| `GEMINI_BATCH_CHARACTER_LIMIT` | `14000` | Maximum characters per section-summary batch. |
| `GEMINI_SYNTHESIS_CHARACTER_LIMIT` | `26000` | Maximum characters in the synthesis request. |
| `TUTOR_CONTEXT_CHUNKS` | `6` | Source chunks retrieved per tutor question. |
| `APP_STALE_JOB_MINUTES` | `30` | Age after which an in-progress job is treated as stale on startup. |
| `THYMELEAF_CACHE` | `false` | Set `true` in production/Compose. |
| `POSTGRES_DB` | `examprep` | Compose only: database name created by the `db` service. |

## Configuring Gemini

1. Create an API key in Google AI Studio (<https://aistudio.google.com/apikey>).
2. Put it in `.env` as `GOOGLE_API_KEY=...` (never commit this file).
3. Confirm `GEMINI_MODEL` names a model your key can access. The default is `gemini-3.8-flash`.
4. Restart the application.

Notes:
- The key is read only on the server (`app.ai.api-key`) and is never written into HTML, JavaScript,
  redirects, error pages or logs. It is masked anywhere configuration is logged.
- Gemini is never called from the browser. All calls go through `GoogleGeminiClient` on the server.
- The application starts without a key. AI actions then fail with HTTP 503 and an explanatory page.
- Study generation uses structured JSON output, output-size limits, request timeouts, retries with
  exponential backoff for transient errors, and citation validation against real chunks.
- Uploaded text is sent to Google as untrusted study material. Prompts explicitly state that document
  content is never an instruction, which reduces prompt-injection risk but cannot eliminate it. Generated
  output is validated before display.

## Running the tests

```bash
./mvnw clean verify
```

The suite requires no Docker daemon and no Gemini key:

- Integration tests start a real PostgreSQL 14 instance through `io.zonky.test:embedded-postgres`.
- `GeminiClient` is replaced with a Mockito bean (`@MockitoBean`), so no paid API calls are made.
- Asynchronous executors are replaced by a deterministic test pool (`TestAsyncConfig`); tests wait on
  persisted status with Awaitility.

Coverage includes registration, BCrypt hashing, login failures, protected routes, logout, PDF/DOCX/PPTX/
Markdown extraction with real page and slide references, invalid and oversized uploads, corrupted and
scanned PDFs, ownership isolation, study generation with mocked Gemini, invalid AI JSON, exam scoring,
answer-key secrecy, idempotent resubmission, tutor citation filtering and persistence, and the unit-level
chunking, retrieval, citation and content-assembly rules.

See [docs/testing.md](docs/testing.md) for the full inventory and manual verification steps.

## Supported documents

| Format | Extensions | Extraction |
| --- | --- | --- |
| PDF | `.pdf` | PDFBox, page by page; `Page 1`, `Page 2`, ... |
| Word | `.docx`, `.doc` | Tika/POI; headings become `Section: ...` references where available |
| PowerPoint | `.pptx`, `.ppt` | POI for PPTX slides (`Slide 1`, ...), Tika for legacy PPT |
| Plain text | `.txt`, `.md` | Headings recognised from Markdown `#` lines |
| Rich text | `.rtf` | Tika |
| OpenDocument | `.odt`, `.odp` | Tika |

Limits and behaviour:
- Maximum 25 MB per file by default. Extension and actual content are both validated.
- Empty, corrupted, unsupported or unreadable files fail with a readable explanation.
- PDFs with no extractable text (typically scans) are reported as scanned documents. OCR is not included
  in this release.
- References are only emitted when the format genuinely provides them. Plain-text formats use
  `Document` or section headings rather than invented page numbers.

## Security and privacy

- BCrypt password hashing; no plaintext passwords are stored or logged.
- Session authentication with `HttpOnly`, `SameSite=Lax` cookies and session fixation migration.
- CSRF protection on all state-changing browser requests, including the JSON upload and flashcard review
  endpoints.
- Ownership checks on every user-specific read and write, at the service and repository level. Unknown or
  foreign IDs return 404.
- Uploads: allow-listed extensions, Tika content detection, size limits, UUID storage names, and a
  normalised path check that prevents traversal. The storage directory is outside the web root and has no
  public URL.
- XSS: Thymeleaf escapes all generated text by default; no `th:utext` is used for AI or student content.
- Prompt injection: document content is wrapped and labelled as untrusted study material in every prompt;
  the system instruction is fixed by the application.
- Safe logging: document contents, tutor messages and API keys are never logged. Exceptions are logged
  server-side and rendered as generic, safe messages.
- AI output validation: typed DTOs, size limits, required-field checks and citation validation.
- Privacy notice: the landing page and study pages explain that extracted text is processed by Google's
  Gemini API. Students can delete a document and its associated study data at any time.

## Database migrations

Flyway owns the schema and runs automatically on startup. Never edit an applied migration; add a new
`V{n}__description.sql` file in `src/main/resources/db/migration`.

| Migration | Contents |
| --- | --- |
| `V1__users.sql` | `users` table with UUID id, full name, unique email, BCrypt hash, timestamps. |
| `V2__documents.sql` | `documents` and `document_chunks`, statuses, failure information, storage keys, ordering. |
| `V3__study_and_ai.sql` | `study_packages` (JSONB content), `flashcards`, `flashcard_reviews`, `quiz_questions`, `quiz_attempts`, `attempt_answers`, `tutor_messages`. |

Hibernate runs with `ddl-auto: validate`, so a mismatch between code and migrations fails startup instead
of silently altering production tables.

## Project layout

```
src/main/java/com/examprep/
  ExamPrepApplication.java
  config/        AppProperties, AiProperties, async executors, stale-job recovery
  security/      principal, user details, current-user service, security configuration
  user/          account entity, repository, registration service
  document/      domain, repository, validation, storage, parsers, chunker, async processing
  ai/            Gemini gateway, prompts, retrieval, DTOs, validation, study-content assembler
  study/         packages, flashcards, quizzes, attempts, tutor, repositories, state services
  web/           controllers, forms, advice (error handling, model attributes)
  common/        shared exceptions, display helpers

src/main/resources/
  application.yml
  db/migration/  Flyway SQL
  templates/     layout, auth, dashboard, documents, study, exam, tutor, error
  static/        css/app.css, js/*.js, images/mark.svg

src/test/java/com/examprep/
  support/       embedded PostgreSQL, fixtures, MockMvc base, test async config
  domain/        unit tests for chunking, retrieval, citations, content assembly
  document/      ingestion integration tests
  study/         generation, scoring, tutor, isolation integration tests
  web/           authentication and route tests
```

## Manual verification checklist

Things that need your own credentials or Docker host and therefore cannot be automated here:

1. `cp .env.example .env`, set a real `GOOGLE_API_KEY`, then `docker compose up --build`.
2. Register a student, upload a real PDF from your course, wait for `READY`.
3. Generate a study package with a real Gemini call; check summary, notes, definitions, flashcards,
   practice questions and mock exam all populate.
4. Sit a mock exam, confirm no answers are visible before submission, then check the results page.
5. Ask the tutor a question the document answers, and one it does not; confirm the second admits the
   material is insufficient.
6. Sign in as a second student and confirm the first student's document and package IDs return 404.
7. Delete a document and confirm its packages disappear too.
8. Restart the application during processing and confirm no job remains stuck.

## Known limitations

- **Docker/Compose were not executed in the development environment used to build this project.** The
  Dockerfile and Compose file are provided and reviewed, but no Docker daemon was available there, so the
  image build and container startup still need to be verified on your machine.
- **No live Gemini call was made during development** because no API key was available. Gemini behaviour
  is covered with mocked responses and the real client's configuration path is tested; a live smoke test
  is required (see above).
- Scanned/image-only PDFs are detected and rejected; OCR is out of scope for this release.
- No vector database: tutor retrieval is lexical overlap over stored chunks. It is deliberately simple and
  works well for focused study questions, but it is not semantic search.
- Legacy `.doc` and `.ppt` extraction depends on Tika and may lose structure; page/slide references are
  only produced when the format exposes them.
- No email verification, password reset or social login.
- Single-instance design: async work is queued in-process and recovered from persisted states on restart.
  Running multiple application instances against the same database without a shared queue is not
  supported.
- Generated content is educational assistance, not an authoritative replacement for the source material.

## Troubleshooting

**Startup fails with a schema-validation error.**
Hibernate validated the entities against the database and found a mismatch. Confirm Flyway ran against the
same schema and that no migration was edited after being applied. `./mvnw clean verify` exercises the full
migration chain against embedded PostgreSQL.

**Uploads fail with "File too large".**
Raise `APP_MAX_UPLOAD_SIZE_MB`. It must be set for the application container as well as any reverse proxy;
the Compose file passes it through.

**Study generation fails with a configuration error.**
`GOOGLE_API_KEY` is empty. Set it and restart. ExamPrep intentionally does not fake AI output.

**Study generation fails with "The AI service could not complete that".**
Check the server log for the request-level cause: model access, quota/rate limit, timeout, or invalid
structured output. Retry transient errors after a pause. The package can be regenerated from the document
page.

**Port already in use.**
Set `SERVER_PORT` (and the Compose mapping) to a free port.

---

ExamPrep is a study aid. Always check generated material against your original documents and your
lecturer's guidance.
