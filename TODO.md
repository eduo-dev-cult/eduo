# TODO

## Open — surfaced in this branch (front-backend-rest-sync)

### Bugs / correctness

- [ ] `configApi.js` (eduo-frontend/src/api/configApi.js:5-9) caches the `fetch` promise in `configPromise` for the lifetime of the page, but never resets it on failure. If the first `/config` request fails (backend not yet up, transient network error), the rejected promise is cached and every subsequent `uploadMaterial` call rejects without retrying. Either clear `configPromise` in a `.catch`, or re-fetch on rejection.
- [ ] `configApi.js` calls `.json()` without checking `res.ok`. A non-2xx response (e.g. 500) would still parse JSON and yield `{maxFileSizeBytes: undefined}`, making `file.size > undefined` always false — silently disabling the client-side check. Add an `if (!res.ok) throw …` guard.

### Code hygiene

- [ ] Inconsistent error-body schema in `GlobalExceptionHandler` (GlobalExceptionHandler.java:25-31 vs 33-54): `handleFileTooLarge` uses `{status, message, timestamp}` while `handleValidationErrors` uses `{status, error, fields, timestamp}`. Pick one key (`message` vs `error`) and apply consistently so the frontend can parse uniformly.
- [ ] `handleFileTooLarge` hardcodes `"status", 413` — use `HttpStatus.CONTENT_TOO_LARGE.value()` to avoid drift.
- [ ] `MaxUploadSizeExceededException ex` parameter is unused in `handleFileTooLarge`. Either log `ex.getMessage()` (it contains the actual offending size) or drop the parameter name.
- [ ] `ConfigController.java` and `configApi.js` both end without a trailing newline — minor style nit.
- [ ] `API_BASE_URL = "http://localhost:8080"` is now duplicated across four frontend API files (configApi.js, materialsApi.js, generationsApi.js, collectionsApi.js). Hoist to a shared module before the count grows further — pre-existing pattern, but this branch added the fourth copy.

### Open questions / design

- [ ] `/config` endpoint is unauthenticated and currently exposes only the upload limit. Fine for now, but document the intent (public config vs. user-scoped config) before adding more fields — anything user- or env-sensitive should not land here.

## Open — surfaced in earlier branch (db-generation-settings-persist)

### Bugs / correctness

- [ ] `CreateGenerationRequest.questions` field with `@AssertTrue` (CreateGenerationRequest.java:38-39) — forces every client to send `"questions": true`. Comment says "must always generate questions" but if always-true, the field carries no information. Decide: drop the field (and the matching `questions` column in `Generation`), or document the intent and validate it consistently.
- [ ] `StudyQuestionService.generateStudyQuestions` (StudyQuestionService.java:34) processes only `request.sourceMaterials()[0]` even though the request accepts a list and `createGeneration` persists join rows for all of them. Either restrict the request to a single material or feed all materials into the prompt.

### Code hygiene before opening the PR

- [ ] Three unresolved XSS fixme comments without investigation (UserController.java:29 `//fixme xss warning`, CollectionController.java:50 `//fixme ide reports xss risk in method, might be false positive`, CollectionController.java:88 `//fixme ide reports xss risk in method`). All three are false positives: DTO serialization goes through Jackson (which escapes JSON), and the filename is already sanitised in `downloadMaterial` before use in a header. Document this conclusion and remove the fixme comments.
- [ ] `SettingsService.allSettings` output formatting glitches (SettingsService.java):
  - line 20: prefixes value with `"Language: "` even though the section header `"LANGUAGE:"` already labels it (redundant).
  - line 23: `"Focus area =  "` has a double space.
  - Casing/style is inconsistent across sections: lowercase difficulty tokens vs. `"true/false"` vs. `"multiple choice"`. Worth a quick unit test that pins the rendered prompt fragment.
- [ ] `application.properties` ships with `spring.profiles.active=mock` and a comment `# fixme should be ollama in prod` (line 51). Decide before merge: either leave `mock` as the default and let prod override via env/profile, or flip the default. Either way, remove the fixme.
- [ ] Test name `createGeneration_returns201_withEmptySourceMaterials` (CollectionControllerTest.java:252) is misleading — the body passes a non-empty `material.getId()` and asserts on the mock LLM's Swedish return string. Rename to reflect what it actually exercises.
- [ ] OllamaLlmServiceTest: dead commented-out disabled strategy at lines 62-63 (`@DisabledIfEnvironmentVariable` variant plus `//llm-suggested alternative^` label). The active `@Disabled` is fine; remove the two dead lines. Also remove the multi-line JavaDoc block on the test method (lines 65-70) — a single-line comment stating the skip condition is sufficient per project conventions.

### Open questions / design

- [ ] `CreateGenerationRequest.sourceMaterials` is a `UUID[]` (CreateGenerationRequest.java:16). Switch to `List<UUID>` for consistency with the rest of the codebase and to make MapStruct/Jackson less surprising.
- [ ] `Generation` entity has three columns flagged `// TODO clearer column name?` (lines 89/93/97): `correct_answers`, `explanations`, `description`. `description` in particular collides semantically with "the quiz description" the same setting controls. Decide naming before the schema ships to prod (cheap now, expensive later).

## Open — pre-existing (carried over)

- [ ] `downloadMaterial` — `collectionId` path variable is declared but not consumed; collection existence not validated. Disabled test pending decision on whether the endpoint needs to stay.
- [ ] `createGeneration` prompt building — UTF-8 decoding of all file types is incorrect for non-plaintext materials (e.g. PDFs). Known issue; endpoint is incomplete pending proper LLM integration system.
- [ ] `CollectionService.deleteSourceMaterial` (line 132) and `deleteGeneration` (line 192) silently succeed on missing IDs, breaking the "no silent no-ops" convention used elsewhere in the service. `// TODO throw if missing?` markers exist on both.

## Closed in this branch

- [x] No validation that `topics` is non-empty when `focusArea == TOPICS` — added `@AssertTrue isTopicsSpecifiedWhenFocusAreaIsTopics()` cross-field check and covering tests (commits `f86f162`, `b5815af`).
- [x] Regression: `AuthService.createUser` gave every new user a default collection; the persisted `Collection` (referencing `User`) remained in the persistence context when `deleteUser` called `userRepository.deleteById`, causing a flush-time FK violation. Fixed by adding `@OnDelete(action = OnDeleteAction.CASCADE)` to `Collection.owner` so the DB-level cascade mirrors `UserCredential`'s (commit `a428fc3`).
- [x] Commented-out scaffolding in `CollectionController.createGeneration` (old lines 121-126) removed; method is now clean.
- [x] Step-by-step "step 1 / step 2 / step 3" scaffolding comments in `createGeneration` removed.
- [x] Branch was behind `origin/dev` (missing `a17e725`); merged via `5c05c59` — PR diff is now clean.
- [x] `CreateGenerationRequest.isAtLeastOneQuestionTypeSelected()` included `questions` (always-true) and omitted `multipleChoice` → validator always passed. Fixed: now checks `openEnded || trueFalse || multipleChoice` (commit `100e554`).
- [x] `CollectionMapper.toDto(Generation)` missing `@Mapping(target = "collectionId", source = "collection.id")` → `collectionId` was null when fetching a collection but populated when fetching a generation directly. Fixed with regression test (commits `8eaccd6`, `7c69869`).
- [x] `StudyQuestionService` built the response DTO manually via placeholder variable `argh`, re-fetching the quiz separately. Fixed: call `generation.setQuiz(quiz)` in-memory then a single `generationMapper.toDto(generation)` suffices (commit `5eb1be6`).
- [x] Commented-out debug print statements and Swedish scaffold comments in `StudyQuestionService` removed (commit `5eb1be6`).
- [x] Stale `//fixme should have content` on `createGeneration` return removed (commit `abb71af`).
- [x] Dead service-style method `deleteSourceMaterial` accidentally pasted into `CollectionServiceTest` removed (commit `bb50b92`).
- [x] `CreateCollectionRequest` had no validation; manual null-check in controller replaced with `@NotNull`/`@NotBlank` constraints and `@Valid` at the boundary (commits `ed250bb`, `a7d34ae`, `e228146`).
- [x] `GlobalExceptionHandler` extended to return a structured JSON body (field→message map + timestamp) for `MethodArgumentNotValidException` (commit `6051477`).
- [x] `CreateGenerationRequest` had no defaults for the optional `correctAnswers` / `explanations` / `description` flags → switched to boxed `Boolean` with a compact-constructor default-to-false (commit `47d0562`).
- [x] `GenerationDto` was `@Value` (immutable) which blocked `studyQuestionService` from setting `quiz` on the DTO before returning → changed to `@Getter` + `@Setter` (commit `58ebf03`).
- [x] `GenerationDto.collection` (full `CollectionDto`) created a `CollectionDto ↔ GenerationDto` mapping cycle previously patched with `@Mapping(target = "collection", ignore = true)` → flattened to `UUID collectionId`, ignore-mapping removed (commit `15d250f`).
- [x] `createGeneration` POST now actually invokes the LLM pipeline via `StudyQuestionService` and returns the generated content in the body (commit `659b833`).
- [x] DTO request records moved into their own `dto.request` package; `CreateGenerationRequest`'s redundant `UUID collection` field dropped (it duplicates the path variable).
- [x] `@Valid` is now enforced on `CreateGenerationRequest` at the controller boundary; `spring-boot-starter-validation` added.
- [x] Per-test-data factory consolidated in `TestDataGenerator` instead of repeated request literals across tests.

## Closed previously

- [x] `UserController` raw integer status codes (`201`, `409`, `401`) → replaced with `HttpStatus` constants
- [x] `ProjectController` redundant `catch (EntityNotFoundException)` blocks → removed; `GlobalExceptionHandler` handles uniformly
- [x] `ProjectDto.userIdId` typo → fixed to `ownerId`
- [x] `QuizDto.quiz_id` snake_case → fixed to `quizId`
- [x] `Project.userId` field → renamed to `owner`
- [x] `UserController` injecting `UserRepository` directly → removed, `GET /users/{id}` endpoint removed
- [x] `GlobalExceptionHandler` mapping all `IllegalArgumentException` to 409 → replaced with `UsernameAlreadyExistsException`; handler now only covers `EntityNotFoundException → 404`
- [x] `createProject` NPE on null name → null check added
- [x] `AuthService.LogInUser` / `DeleteUser` PascalCase → renamed to `logInUser` / `deleteUser`
- [x] `deleteProject`, `uploadMaterial`, `createGeneration`, `getQuiz`, `updateQuiz`, `deleteQuiz` missing 404 handling → covered by restored `GlobalExceptionHandler`