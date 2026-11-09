# Module contracts

This document records the interfaces and DTOs that the modules share. It exists so that the web layer,
the AI module and the study module can evolve without importing each other's entities or SDK types.

Contract rules:

- Business logic lives behind the interfaces below. Controllers inject them; they never inject
  repositories or the Gemini SDK.
- Immutable data crosses module boundaries as Java records.
- JPA entities never leave a service.
- Changing a record field or method signature is a cross-module change: update every implementation and
  this document in the same commit.
- Ownership is always enforced inside the service from the authenticated user id, never from a request
  parameter supplied by the browser.

## 1. `security.CurrentUserService`

```java
UUID requireCurrentUserId();
```

The single source of the signed-in student's id. Throws an authentication exception when there is no
authenticated principal. Thymeleaf uses `StudentPrincipal` (`fullName`, `initials`, `getUsername()`);
`StudentUserDetailsService` loads accounts by email.

## 2. `document.service.DocumentApplicationService`

```java
DocumentUploadResult upload(MultipartFile file, UUID ownerId);
DocumentStatusDto   getStatus(UUID documentId, UUID ownerId);
List<DocumentSummaryDto> listDocuments(UUID ownerId);
DocumentContext     getContext(UUID documentId, UUID ownerId);
void                delete(UUID documentId, UUID ownerId);
```

Behavioural contract:

- `upload` validates extension, size and detected content type, stores the file privately, inserts the
  row as `UPLOADED` and schedules extraction. It returns immediately; it never waits for extraction.
- `getStatus`, `getContext` and `delete` throw `ResourceNotFoundException` when the id does not exist or
  belongs to another student. Foreign ids are indistinguishable from missing ids.
- `getContext` is the AI module's only source of document text. It returns ordered chunks with real
  references, and fails if the document is not `READY`.
- `delete` removes packages derived from the document, its chunks, the row and the stored file.

### Document DTOs (`document.dto.DocumentDtos`)

| Record | Fields |
| --- | --- |
| `DocumentUploadResult` | `UUID documentId`, `String originalFilename`, `String status`, `String message` |
| `DocumentStatusDto` | `UUID documentId`, `String originalFilename`, `String status`, `int progressPercent`, `String message`, `String failureReason`, `boolean ready`, `Integer pageCount`, `long chunkCount`, `Integer characterCount` |
| `DocumentSummaryDto` | `UUID documentId`, `String originalFilename`, `String contentType`, `String status`, `long sizeBytes`, `String sizeLabel`, `Integer pageCount`, `long chunkCount`, `String failureReason`, `String createdAtLabel`, `Instant createdAt` |
| `DocumentChunkDto` | `UUID chunkId`, `String reference`, `String text` |
| `DocumentContext` | `UUID documentId`, `String title`, `List<DocumentChunkDto> chunks` |

`reference` is a display-safe source label produced by extraction: `Page 3`, `Slide 7`,
`Section: <heading>`, `Document`, or `<label> (part 2)` when a section is split. It is never invented.

`DocumentStatus` enum: `UPLOADED`, `PROCESSING`, `READY`, `FAILED`.
`failureReason` is always safe to render: no stack traces, paths, prompts or document text.

## 3. `ai.client.GeminiClient`

```java
String generate(GeminiRequest request);
```

- Implemented by `GoogleGeminiClient`; the only place that touches `com.google.genai`.
- Throws `AiConfigurationException` when `GOOGLE_API_KEY` is blank, before any network access.
- Throws `AiServiceException` for provider/transport failures after bounded retries, and
  `AiResponseException` when the returned content is empty or unusable.
- Never logs prompt text, document text or the key.

`GeminiRequest` carries the system instruction, user prompt, temperature and optional JSON mode/timeout
overrides. Higher layers build requests through `ai.prompt.PromptLibrary`, which owns the fixed system
instructions, the untrusted-document delimiters and the JSON schema description.

### AI DTOs

`ai.dto.AiStudyContent` mirrors the model's JSON with nullable/permissive fields so malformed output can
be validated instead of crashing deserialisation:

- `title`, `introduction`, `overallSummary`
- `learningObjectives`, `revisionPriorities`
- `topics`: `name`, `explanation`, `keyConcepts`, `sourceReferences`
- `revisionNotes`: `heading`, `body`, `sourceReferences`
- `definitions`: `term`, `definition`, `sourceReference`
- `formulas`: `name`, `expression`, `explanation`, `sourceReference`
- `flashcards`: `front`, `back`, `sourceReference`
- `practiceQuestions` / `mockQuestions`: `type`, `prompt`, `options`, `correctAnswer`, `explanation`,
  `difficulty`, `sourceReference`
- `AiSectionSummary`: `summary`, `keyPoints` (intermediate hierarchical step)

Validation and normalisation happen in `ai.service.StudyContentAssembler`, which returns
`study.domain.content.StudyContent.GeneratedPackage`. Invalid questions are dropped, citations are
filtered by `ai.validation.CitationValidator`, types and difficulties are mapped to enums, and a package
without the required minimum content raises `AiResponseException`.

## 4. `study.service.StudyPackApplicationService`

```java
StudyPackCreationResult create(UUID documentId, UUID ownerId, StudyPreferences preferences);
StudyPackStatusDto      getStatus(UUID studyPackId, UUID ownerId);
StudyPackDto            getStudyPack(UUID studyPackId, UUID ownerId);
List<StudyPackSummaryDto> listRecent(UUID ownerId);
List<AttemptSummaryDto>   recentAttempts(UUID ownerId);
StudyProgressDto          progress(UUID ownerId);
QuizResultDto           submitQuiz(UUID attemptId, UUID ownerId, QuizSubmissionDto submission);
TutorAnswerDto          askTutor(UUID studyPackId, UUID ownerId, TutorQuestionDto question);
void                    delete(UUID studyPackId, UUID ownerId);
```

`submitQuiz` takes the attempt id rather than the package id: an attempt is the subject of scoring, and
the service resolves its package and enforces ownership through the attempt. This is a deliberate change
from the initial sketch so that a student cannot post answers against a different package.

Behavioural contract:

- `create` requires a `READY` document owned by the caller, inserts `QUEUED`, schedules generation and
  returns immediately.
- `getStudyPack` throws `ResourceNotFoundException` unless the caller owns the package. It does not expose
  correct answers for mock exams.
- Exam pages are built from `ExamView`, which contains stems and options only. Answers and explanations
  are reachable exclusively through `getResults` after submission.
- `submitQuiz` is idempotent: a submitted attempt returns its original result and cannot be rescored.
- `askTutor` persists the student message and the assistant answer with its validated citations.
- `delete` removes the package, its flashcards, questions, attempts, tutor messages and reviews.

### Study enums

- `StudyPackageStatus`: `QUEUED`, `PROCESSING`, `READY`, `FAILED`
- `QuestionSection`: `PRACTICE`, `MOCK`
- `QuestionType`: `MULTIPLE_CHOICE`, `TRUE_FALSE`, `SHORT_ANSWER`
- `Difficulty`: `EASY`, `MEDIUM`, `HARD`
- `AttemptStatus`: `IN_PROGRESS`, `SUBMITTED`
- `MessageRole`: `USER`, `ASSISTANT`

### Study DTOs (`study.dto.StudyPackDtos`)

| Record | Fields |
| --- | --- |
| `StudyPackCreationResult` | `UUID studyPackId`, `String status`, `String message` |
| `StudyPackStatusDto` | `UUID studyPackId`, `UUID documentId`, `String status`, `int progressPercent`, `String message`, `String failureReason`, `boolean ready` |
| `StudyPackDto` | ids and titles, `status`, `introduction`, `overallSummary`, objectives, priorities, `List<Topic>`, `List<NoteSection>`, `List<Definition>`, `List<Formula>`, counts, preferences, timestamps, failure/progress text, `ready`, `model` |
| `StudyPackSummaryDto` | `studyPackId`, `documentId`, `title`, `documentTitle`, `status`, `createdAtLabel`, counts, `ready` |
| `FlashcardView` | `flashcardId`, `position`, `front`, `back`, `sourceReference` |
| `PracticeQuestionView` | `questionId`, `type`, `prompt`, options, `correctAnswer`, `explanation`, `difficulty`, `sourceReference` |
| `ExamQuestionView` | `questionId`, `position`, `type`, `prompt`, options, `difficulty`, `sourceReference` — **no answer** |
| `ExamView` | `attemptId`, `studyPackId`, `studyPackTitle`, `status`, `allowedSeconds`, `totalQuestions`, start timestamps, `List<ExamQuestionView>` |
| `QuizSubmissionDto` | `Map<String, String> answers` keyed by question id |
| `QuestionResult` | question fields plus `submittedAnswer`, `correctAnswer`, `Boolean correct`, `explanation` |
| `QuizResultDto` | attempt/package ids, `scorePercent`, counts, duration, submission label, feedback headline, revision areas, `List<QuestionResult>` |
| `AttemptSummaryDto` | `attemptId`, package id/title, `scorePercent`, counts, timestamps |
| `StudyProgressDto` | `studyPackages`, `completedExams`, `averageScorePercent`, `bestScorePercent`, `flashcardsReviewed`, `documents` |
| `TutorQuestionDto` | `String question` |
| `TutorAnswerDto` | `String answer`, `List<String> sourceReferences`, `boolean insufficientInformation`, `createdAtLabel` |
| `TutorMessageView` | `role`, `content`, `sourceReferences`, `createdAtLabel` |
| `TutorConversationDto` | `studyPackId`, `studyPackTitle`, `status`, `List<TutorMessageView>` |

`study.dto.StudyPreferences(String language, String academicLevel, LocalDate examDate,
int practiceQuestionCount, int mockQuestionCount, int flashcardCount)` is normalised by the service:
blank values fall back to English/Undergraduate, counts are clamped (practice 5–30, mock 5–25, flashcards
5–40) and strings are length-limited. `StudyPreferences.defaults()` is the canonical default.

## 5. Web endpoints consumed by the browser

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/` | Landing page |
| GET | `/login`, `/register` | Auth pages |
| POST | `/register`, `/login`, `/logout` | Account actions (CSRF required) |
| GET | `/dashboard` | Recent documents, packages, attempts and progress |
| GET | `/documents` | Document list and upload UI |
| POST | `/documents/upload` | Multipart upload (form fallback) |
| POST | `/documents/upload-json` | Upload with JSON result for drag-and-drop |
| GET | `/documents/{id}/status` | Poll extraction status |
| POST | `/documents/{id}/delete` | Delete document and derived data |
| POST | `/documents/{id}/study-packages` | Queue package generation |
| GET | `/study/{id}` | Overview |
| GET | `/study/{id}/topics`, `/notes`, `/definitions`, `/formulas`, `/exam` | Package sections |
| GET | `/study/{id}/status` | Poll generation status |
| POST | `/study/{id}/delete` | Delete package |
| GET | `/study/{id}/flashcards` | Flashcard viewer |
| POST | `/study/{id}/flashcards/review`, `/review-json` | Record a review |
| GET | `/study/{id}/practice` | Practice questions |
| POST | `/study/{id}/mock-exams` | Start a timed attempt |
| GET | `/exams/{attemptId}` | Take an exam (no answer key) |
| POST | `/exams/{attemptId}/submit` | Submit answers, server-side scoring |
| GET | `/exams/{attemptId}/results` | Score, review and explanations |
| GET | `/study/{id}/tutor` | Conversation view |
| POST | `/study/{id}/tutor` | Ask the tutor |

JSON responses are small and typed. Failures use the global error pages; the JSON upload endpoint returns
`{"error": "..."}` with a 4xx status so the upload UI can show it inline.

## 6. Error contract

| Exception | Status | Meaning |
| --- | --- | --- |
| `ResourceNotFoundException` | 404 | Missing or foreign resource; never distinguishes the two |
| `InvalidRequestException`, `InvalidUploadException` | 400 | Invalid input or file |
| `MaxUploadSizeExceededException` | 413 | Container-level size rejection |
| `AccessDeniedException` | 403 | Authenticated but not permitted |
| `AiConfigurationException` | 503 | `GOOGLE_API_KEY` missing; no fake output |
| `AiServiceException`, `AiResponseException` | 502 | Provider failure or unusable output |
| anything else | 500 | Logged with context; generic message rendered |
| `NoResourceFoundException` | 404 | Unknown page or asset |

Messages rendered to students are curated. Logs may contain exception types and request metadata, never
document contents, prompts, tutor messages or secrets.
