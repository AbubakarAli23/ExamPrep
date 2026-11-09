# Testing ExamPrep

## How to run the suite

```bash
./mvnw clean verify
```

This compiles the application, runs every test and packages the executable jar. No Docker daemon, no
external database and no Gemini API key are required.

Useful variations:

```bash
./mvnw test                              # tests only
./mvnw test -Dtest=DocumentIngestionIntegrationTest
./mvnw test -Dtest='StudyContentAssemblerTest#rejectsPackagesThatAreTooIncompleteToBeUseful'
./mvnw -B --no-transfer-progress clean verify   # what CI runs
```

Reports:

- Console summary from Surefire.
- `target/surefire-reports/*.txt` and `*.xml` for per-test details.
- CI uploads `target/surefire-reports/` as an artifact even when the build fails.

## Test infrastructure

| Concern | Approach |
| --- | --- |
| Database | `io.zonky.test:embedded-postgres:2.2.2` starts a real PostgreSQL 14 server (binaries resolved by Maven) for the test JVM. |
| Schema | Flyway runs the real migrations; Hibernate runs with `ddl-auto: validate`, so the tests also verify that the entities match the migrations. |
| AI | `GeminiClient` is replaced with `@MockitoBean`. Responses come from `AiFixtures`; no network call and no API key. |
| Async | The `test` profile disables the production executors; `TestAsyncConfig` provides a deterministic pool, and tests wait on persisted state with Awaitility. |
| Storage | `StorageSupport` points `APP_STORAGE_LOCATION` at a JUnit temporary directory. |
| HTTP | `@SpringBootTest` + `MockMvc` with real Spring Security filters, CSRF tokens and sessions. |
| Files | `TestDocuments` builds real PDFs with PDFBox, real DOCX/PPTX with POI, and deliberately malformed PDFs. |

The test profile also raises the upload limit down to 1 MB and lowers log verbosity
(`src/test/resources/application-test.yml`).

## Inventory

### Authentication and access control — `web/AuthenticationIntegrationTest`

- Landing, login and registration pages render.
- Registration persists an account and the stored value is a BCrypt hash (`$2...`) that is not the
  plaintext password and matches via `PasswordEncoder`.
- Duplicate email shows a friendly validation error.
- Short password is rejected by Bean Validation.
- Incorrect password redirects to `/login?error` and does not establish a session.
- `/dashboard`, `/documents` and `/study/{id}` redirect anonymous users to login.
- Successful login renders the dashboard; POST `/logout` ends the session.
- CSS and JavaScript assets are served.

### Application routes and error handling — `web/ApplicationRoutesTest`

- Unknown paths render the friendly error page with HTTP 404.
- Empty dashboard and document list render their empty states.

### Document ingestion — `document/DocumentIngestionIntegrationTest`

- PDF upload: extraction reaches `READY`, chunks contain real `Page 1` / `Page 2` references and the
  extracted words.
- DOCX upload: chunks are produced and the reference is the document-level label.
- PPTX upload: `Slide 1` and `Slide 2` references exist.
- Markdown upload: `#` headings become `Section: <heading>` references.
- Unsupported extension is rejected at upload with 400 and an explanatory JSON error.
- Oversized upload is rejected with 400 and a message mentioning the limit.
- Malformed PDF fails processing with a readable, non-empty failure message.
- Blank (scanned-style) PDF fails with an explanation that mentions scanning.
- Another student cannot read the status endpoint (404) or start generation from the document
  (redirect with an error notice).
- Deleting a document removes the database record (and the stored file).

### Study generation, assessment and tutor — `study/StudyWorkflowIntegrationTest`

- Mocked Gemini generation reaches `READY`, persists title, summary, topics, notes and exactly the mock
  exam questions; every study page (overview, topics, notes, definitions, formulas, flashcards, practice,
  exam, tutor) renders, including flashcard content and short-answer controls.
- Starting a mock exam creates an attempt and renders the exam page; the page contains neither the
  `Correct answer:` markers nor a distinctive explanation string, proving the answer key is not sent to
  the browser.
- Submitting all correct answers yields 100%, 2 gradable questions, 2 correct, and a results page with the
  score, answer review and a note that short answers are not auto-scored.
- Resubmitting cannot change the score or the submission timestamp (idempotent).
- Tutor question persists a user message and an assistant message; invented `Page 999` is filtered while
  the real `Page 1` is kept and rendered.
- A second student gets 404 for the package, its notes, its status and its exam start endpoint.
- Invalid AI JSON fails the package with a message naming Gemini, leaves no questions and no tutor
  messages, and renders the failure state instead of invented content.
- A document that failed extraction cannot start generation and no package row is created.
- A missing API key raises `AiConfigurationException` mentioning `GOOGLE_API_KEY` rather than producing
  fake output.
- A generated package appears on the dashboard, the document list shows the original filename, and the
  notes page contains generated content.

### Unit tests — `domain/`

- `DocumentChunkerTest`: page references are preserved, split pages get `(part n)`, whitespace is
  normalised, empty sections are dropped, oversized paragraphs are hard-split and every chunk stays under
  the size ceiling.
- `CitationValidatorTest`: base labels of split chunks are accepted, duplicates removed, case/whitespace
  normalised, fabricated labels rejected, and an empty allow-list rejects everything.
- `ChunkRetrieverTest`: the chunk discussing the question ranks first, unrelated queries fall back to the
  opening material, empty documents return nothing.
- `StudyContentAssemblerTest`: a complete AI payload becomes a validated package; invalid citations are
  dropped; multiple-choice questions whose answer is not an option are discarded; true/false answers are
  normalised; hopelessly incomplete payloads raise `AiResponseException`.

## What is deliberately not automated

- **Real Gemini calls.** They require a funded key and are non-deterministic. The client's configuration
  behaviour is tested; live behaviour must be verified manually.
- **Docker image and Compose stack.** The build environment had no Docker daemon. Run
  `docker compose up --build` on a machine with Docker and follow the manual checklist in the README.
- **Visual review at specific breakpoints.** The CSS is responsive by construction, but no automated
  screenshot or visual-regression suite is included.
- **Email delivery, password reset, MFA.** Out of scope for this release.

## Manual verification procedure

1. **Configuration.** `cp .env.example .env`; set `GOOGLE_API_KEY` to a real key; keep
   `GEMINI_MODEL=gemini-3.8-flash` or choose another model your key can access.
2. **Start.** `docker compose up --build`; wait for the app log line indicating the server started; open
   <http://localhost:8080>.
3. **Account.** Register, sign out, sign in with a wrong password (expect a friendly error), then sign in
   correctly.
4. **Upload.** Upload a text-based PDF and a PPTX. Watch the status move `UPLOADED` → `PROCESSING` →
   `READY`. Try a `.zip` (rejected) and a scanned PDF (clear scanned-document explanation).
5. **Generation.** Start a study package. Confirm the status polls until `READY` and that the overview,
   topics, notes, definitions and formulas are populated from your document rather than generic text.
6. **Assessment.** Open the mock exam, confirm no answers appear in the page source before submission,
   submit, and check the score and explanations. Submit short answers and confirm the manual-review note.
7. **Tutor.** Ask a question answered by the document; check citations point at real pages. Ask something
   absent; confirm the answer says the material does not contain it.
8. **Isolation.** With a second account, request the first account's document status, study package and
   attempt IDs; expect 404.
9. **Restart safety.** Start a generation, restart the application while it is `PROCESSING`, and confirm
   the job becomes `FAILED` with a retry explanation rather than staying stuck.
10. **Deletion.** Delete the document and confirm its study packages disappear from the dashboard and the
    uploads volume no longer holds the file.
