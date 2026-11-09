# ExamPrep architecture

ExamPrep is a modular monolith: one Spring Boot process, one deployable, clear internal module
boundaries. The browser is rendered by Thymeleaf; plain JavaScript adds only the interactions that
server-rendered HTML cannot provide.

## 1. Runtime view

```
Browser
  │  HTML forms, fetch() for JSON endpoints, session cookie, CSRF header
  ▼
Spring Security filter chain
  │  session authentication, CSRF, ownership context
  ▼
web/controller ──► application services ──► repositories ──► PostgreSQL
                        │
                        ├── document/  validation, private storage, extraction, chunking
                        ├── ai/        Gemini gateway, prompts, retrieval, output validation
                        └── study/     packages, flashcards, quizzes, attempts, tutor

Bounded executors (document-, study-) run extraction and generation off the request thread.
State lives in PostgreSQL; the browser polls status endpoints.
```

The servlet container, Thymeleaf and the static assets are all part of the same Spring Boot
application. There is no Node build, no SPA and no API gateway.

## 2. Modules and package ownership

| Package | Responsibility | Depends on |
| --- | --- | --- |
| `config` | `AppProperties`, `AiProperties`, bounded executors, startup recovery | — |
| `security` | student principal, user-details service, current-user service, security chain | `user` |
| `user` | accounts, BCrypt hashing, registration validation, duplicate-email rule | `common` |
| `document` | upload validation, private storage, PDF/PPTX/Tika extraction, chunking, async state machine | `config`, `common` |
| `ai` | Gemini gateway and prompts, chunk retrieval, JSON parsing and validation, citation checks | `document` DTOs |
| `study` | package generation orchestration, generated-content persistence, flashcards, quizzes, attempts, tutor | `ai`, `document` |
| `web` | controllers, forms, model attributes, global error handling | all application services |
| `common` | shared exceptions, display helpers | — |

Rules enforced by the code:

- Controllers never touch repositories or SDK classes; they call the `*ApplicationService` contracts.
- `ai` never imports `study`; the study generator consumes the AI assembler's typed output.
- The document module has no dependency on Gemini. Extraction and generation fail independently.
- JPA entities are never returned from controllers; DTO records cross layer boundaries.

## 3. Persistence model

Flyway owns the schema (`src/main/resources/db/migration`); Hibernate runs `ddl-auto: validate`, so a
drift between entity and migration fails startup.

- All primary keys are UUIDs generated in the application.
- Foreign keys are stored as scalar UUID columns rather than JPA associations. Queries are explicit,
  always include the owner, and there is no lazy-loading state crossing a transaction boundary.
- Generated learning content is stored as typed JSONB on `study_packages` and deserialised into records.
  Flashcards, questions, attempts, answers and tutor messages are relational because they are queried,
  scored and counted independently.
- Timestamps are `timestamptz` in UTC; `hibernate.jdbc.time_zone` is UTC.
- Constraints, unique indexes and check constraints live in SQL where they belong (for example unique
  email, unique chunk ordering per document, status values).

### Document states

```
UPLOADED ──► PROCESSING ──► READY
                   └──────► FAILED (safe failure_reason)
```

`UPLOADED` is committed before the event is published. The async listener transitions to `PROCESSING`,
extracts, chunks and persists, then commits `READY` in the same transaction as the chunks. Any exception
is recorded as `FAILED` with a message safe to display.

### Study package states

```
QUEUED ──► PROCESSING ──► READY
                └───────► FAILED (safe failure_reason)
```

Creation validates that the document is `READY` and owned by the caller, inserts `QUEUED`, and publishes
an in-process event. The listener claims the row with a conditional update, so a duplicate delivery cannot
run the same package twice.

### Restart recovery

`StaleJobRecovery` runs as an `ApplicationRunner`:

- Documents `UPLOADED` older than `APP_STALE_JOB_MINUTES` are re-queued.
- Documents and packages stuck in `PROCESSING` beyond the threshold are marked `FAILED` with a retry
  explanation.

This keeps the UI honest after a crash instead of showing a job that will never finish.

## 4. Document pipeline

1. **Validate.** Extension allow-list, non-empty file, size limit, then content detection with Apache
   Tika. A file whose detected type contradicts its extension is rejected. The browser MIME type is
   ignored.
2. **Store.** `LocalFileStorage` writes to the configured private directory under a UUID name with the
   validated extension. `resolve()` normalises and checks that the final path stays inside the storage
   root, which blocks traversal. Files are never copied into `static/` and no download URL exists.
3. **Record.** A `documents` row is inserted with the original filename (metadata only), detected type,
   storage key and `UPLOADED`.
4. **Extract asynchronously** on `documentTaskExecutor`:
   - PDF: PDFBox, one section per page (`Page n`). An empty text layer marks the document as scanned.
   - PPTX: POI, one section per slide (`Slide n`).
   - DOCX/DOC/PPT/TXT/Markdown/RTF/ODT/ODP: Tika, with explicit handling for Markdown headings and
     Word headings/sections, falling back to the document-level label.
   - Corrupted, encrypted or unreadable input raises `ExtractionException`, which becomes a `FAILED`
     state with a readable message.
5. **Chunk.** `DocumentChunker` normalises whitespace, splits sections at paragraph boundaries up to a
   fixed character ceiling, hard-splits single oversized paragraphs, drops empty sections and assigns
   `(part n)` suffixes when a page or slide is split. Every chunk keeps a real reference; plain-text
   formats use `Document` or `Section: <heading>` rather than invented page numbers.
6. **Persist** chunks with their ordering, then commit `READY`.
7. **Delete** removes the study packages, chunks, database row and stored file.

## 5. AI generation

### Gateway

`GeminiClient` is the only seam that knows about the SDK. `GoogleGeminiClient`:

- reads the key and model from `AiProperties`, never from a literal;
- throws `AiConfigurationException` before any network call when the key is blank;
- sets `httpOptions` timeouts, `systemInstruction`, `responseMimeType("application/json")` where JSON is
  required, temperature and `maxOutputTokens`;
- retries only transient conditions (HTTP 429 and 5xx) with exponential backoff up to
  `gemini-max-retries`, and maps the rest to `AiServiceException` / `AiResponseException`;
- logs model, latency and error category only — never the document text, prompt or key.

### Hierarchical generation

Small documents are sent as one synthesis request. Larger ones are split into batches below
`GEMINI_BATCH_CHARACTER_LIMIT`; each batch produces a compact section summary, and the summaries (bounded
by `GEMINI_SYNTHESIS_CHARACTER_LIMIT`) feed the final synthesis request. The final request uses a fixed
system instruction plus a JSON schema description and returns the whole package.

### Validation

`StudyContentAssembler` turns `AiStudyContent` into `StudyContent.GeneratedPackage`:

- required sections (title, summary, topics, notes, questions) must be present in sufficient quantity,
  otherwise `AiResponseException` fails the package;
- citations are filtered through `CitationValidator` against the document's real chunk references, with
  split-chunk base labels accepted; unknown labels are dropped rather than displayed;
- multiple-choice answers must be one of the offered options; invalid questions are discarded;
- true/false answers are normalised to `True`/`False` and given fixed options;
- difficulty and question type are normalised to enums with safe defaults;
- output is trimmed to the requested counts without inventing replacements.

The generator retries once when the payload cannot be parsed or validated, then fails cleanly. Generated
content is never replaced with sample text.

### Prompt-injection posture

Document text is wrapped in delimiters and explicitly labelled as untrusted study material. The system
instruction states that instructions inside the document must be ignored and that only the application
defines the AI's role. Tutor prompts include the same warning and instruct the model to say when the
provided passages are insufficient. Output is validated and rendered as escaped text.

## 6. Study features

- **Flashcards** are persisted per package; review actions update review counters used by the dashboard.
- **Practice questions** are served with answers and explanations because they are a learning tool, and
  the UI keeps the suggested answer hidden until requested.
- **Mock examinations** are created on demand with a server-side duration. The attempt stores the selected
  question set, allowed seconds, start time and status. The exam page renders stems and options only —
  correct answers and explanations are never serialised into the HTML. Submission is scored on the
  server: multiple-choice and true/false count toward the score, short answers are stored for manual
  review. A submitted attempt is immutable; resubmission redirects to the same result.
- **Tutor** retrieves chunks by lexical overlap with the question (with a term-frequency style score and
  document-order fallback), sends the top `TUTOR_CONTEXT_CHUNKS` passages with their references, filters
  the returned citations, and persists both messages for that student and package.

## 7. Web layer

- Thymeleaf layout fragments provide the header, footer, flash area and scripts. Every page shares the
  same visual language and navigation.
- Forms use Bean Validation with field-level errors; server-side validation is authoritative.
- JSON endpoints (`/documents/upload-json`, `/documents/{id}/status`, `/study/{id}/status`,
  `/study/{id}/flashcards/review-json`) return small typed payloads for progressive enhancement.
- CSRF tokens are injected into a meta tag and sent by `app.js`; multipart uploads include the token as a
  form field.
- `GlobalExceptionHandler` maps domain exceptions to friendly pages with correct status codes: 404 for
  missing/foreign resources, 400 for invalid input, 413 for oversize uploads, 403 for access denied, 503
  for missing AI configuration, 502 for AI failures, 500 for anything unexpected. Details are logged, not
  rendered.
- `GlobalModelAttributes` supplies the current year and shared navigation state.

## 8. Concurrency and transactions

- Two bounded executors (`documentTaskExecutor`, `studyTaskExecutor`), each core 2 / max 4 / queue 100.
  Rejection surfaces as a failed state rather than a lost job.
- Async listeners run outside the request transaction; state transitions are short, explicit
  transactions (`DocumentStateService`, `StudyPackageStateService`).
- Uploads commit before the event is published, so a fast worker can always find the row.
- Claiming a queued package uses a conditional update, making duplicate events harmless.
- Generation writes the package, flashcards and questions in one transaction so `READY` never exposes a
  half-populated package.
- Hibernate `open-in-view` is disabled; DTOs are built inside the service transaction.

## 9. Configuration and deployment

- `application.yml` reads every environment variable with a safe default. No secret has a real default.
- The Docker image is multi-stage, built with Maven, and runs as the non-root `examprep` user (UID 1001)
  on a JRE 21 base. Uploads live in `/app/data/uploads`, a mounted volume.
- Compose wires PostgreSQL 16 with a health check and named volumes for data and uploads; the app waits
  for the database to be healthy and lets Flyway migrate on startup.
- The app starts without `GOOGLE_API_KEY`; AI actions then return 503 with a clear configuration message.
- CI (`.github/workflows/ci.yml`) builds with JDK 21 and runs `./mvnw clean verify` on pushes and pull
  requests to `main`.

## 10. Deliberate non-goals

No microservices, message broker, Redis, Kubernetes, vector database, OCR, external search, email
verification or social login. These are excluded to keep the first release reliable and auditable.
