# TODO

## Open — surfaced in this branch (db-generation-settings-persist)

### Bugs / correctness

- [ ] `CreateGenerationRequest.questions` field with `@AssertTrue` (CreateGenerationRequest.java:37-39) — forces every client to send `"questions": true`. Comment says "must always generate questions" but if always-true, the field carries no information. Decide: drop the field, or document the intent and validate it consistently.
- [ ] `StudyQuestionService.generateStudyQuestions` (StudyQuestionService.java:34) processes only `request.sourceMaterials()[0]` even though the request accepts a list and `createGeneration` persists join rows for all of them. Either restrict the request to a single material or feed all materials into the prompt.

### Code hygiene before opening the PR

- [ ] Commented-out scaffolding in CollectionController (`//GenerationDto generationdto = studyQuestionService.generateStudyQuestions(request);` at line 124). Remove.
- [ ] Three unresolved XSS fixme comments without investigation (UserController.java:29 `//fixme xss warning`, CollectionController.java:50 `//fixme ide reports xss risk in method, might be false positive`, CollectionController.java:88 `//fixme ide reports xss risk in method`). Either remediate or document why the warning is a false positive and remove the fixme.
- [ ] Step-by-step scaffolding comments left in `CollectionController.createGeneration` (lines 121-126, "step 1 / step 2 / step 3"). Remove now that the method is implemented.
- [ ] `SettingsService.allSettings` output formatting glitches (SettingsService.java):
  - line 20: prefixes value with `"Language: "` even though the section header `"LANGUAGE:"` already labels it (redundant).
  - line 23: `"Focus area =  "` has a double space.
  - Casing/style is inconsistent across sections: lowercase difficulty tokens vs. `"true/false"` vs. `"multiple choice"`. Worth a quick unit test that pins the rendered prompt fragment.
- [ ] `application.properties:66` ships with `spring.profiles.active=mock` and a comment `# fixme should be ollama in prod`. Decide before merge: either leave `mock` as the default and let prod override via env/profile, or flip the default. Either way, address the fixme.
- [ ] Test name `createGeneration_returns201_withEmptySourceMaterials` (CollectionControllerTest.java:252) is misleading — the body passes a non-empty `material.getId()` and asserts on the mock LLM's Swedish return string. Rename to reflect what it actually exercises.
- [ ] OllamaLlmServiceTest: two disabled strategies coexist as comments (lines 61-63: `@Disabled` variant, `@DisabledIfEnvironmentVariable` variant, plus a `//llm-suggested alternative^` label). The test itself uses `assumeTrue` which is fine; clean up the dead commented alternatives.

### Open questions / design

- [ ] No validation that `topics` is non-empty when `focusArea == TOPICS`. `GenerationFocusArea.format` will print `"Topics: "` with an empty trailing string. Add `@AssertTrue` cross-field check or make `topics` part of the enum's contract.
- [ ] `CreateGenerationRequest.sourceMaterials` is a `UUID[]` (CreateGenerationRequest.java:16). Switch to `List<UUID>` for consistency with the rest of the codebase and to make MapStruct/Jackson less surprising.
- [ ] `Generation` entity has three columns flagged `// TODO clearer column name?` (lines 89/93/97): `correct_answers`, `explanations`, `description`. `description` in particular collides semantically with "the quiz description" the same setting controls. Decide naming before the schema ships to prod (cheap now, expensive later).

### Branch hygiene before opening the PR

- [ ] Branch is behind `origin/dev` — `a17e725 feat: add Windows GPU support for Ollama with updated README and setup script` lives on dev but not on this branch, so `git diff origin/dev..HEAD` shows spurious deletions of `infra/llm/docker-compose.windows-gpu.yml`, GPU-related README sections, and PowerShell GPU flags. Merge `dev` again so the PR diff reflects only the work this branch intends.

## Open — pre-existing (carried over)

- [ ] `downloadMaterial` — `collectionId` path variable is declared but not consumed; collection existence not validated. Disabled test pending decision on whether the endpoint needs to stay.
- [ ] `createGeneration` prompt building — UTF-8 decoding of all file types is incorrect for non-plaintext materials (e.g. PDFs). Known issue; endpoint is incomplete pending proper LLM integration system.
- [ ] `CollectionService.deleteSourceMaterial` (line 132) and `deleteGeneration` (line 192) silently succeed on missing IDs, breaking the "no silent no-ops" convention used elsewhere in the service. `// TODO throw if missing?` markers exist on both.

## Closed in this branch

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