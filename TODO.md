# TODO

## Open — surfaced in this branch (db-generation-settings-persist)

### Bugs / correctness

- [ ] `CreateGenerationRequest.isAtLeastOneQuestionTypeSelected()` (CreateGenerationRequest.java:50-53) — validator checks `openEnded || trueFalse || questions` but **omits `multipleChoice`** AND includes `questions`, which is itself `@AssertTrue` required → validator always passes. Either fix the set of fields it checks, or remove the whole `questions` flag and have the validator cover the three real question types.
- [ ] `CreateGenerationRequest.questions` field with `@AssertTrue` (CreateGenerationRequest.java:36-37) — forces every client to send `"questions": true`. Comment says "must always generate questions" but if always-true, the field carries no information. Decide: drop the field, or document the intent and validate it consistently.
- [ ] `StudyQuestionService.generateStudyQuestions` (StudyQuestionService.java:34) processes only `request.sourceMaterials()[0]` even though the request accepts a list and `createGeneration` persists join rows for all of them. Either restrict the request to a single material or feed all materials into the prompt.
- [ ] `CollectionMapper.toDto(Generation)` (CollectionMapper.java:50) is missing `@Mapping(target = "collectionId", source = "collection.id")`. `GenerationMapper.toDto(Generation)` sets it; `CollectionMapper` does not. Result: `CollectionDto.generations[].collectionId` is `null` when fetching a collection, but populated when fetching a generation directly. Inconsistency between two mappers for the same source→target pair.
- [ ] `StudyQuestionService` builds the response DTO manually because the `Generation` returned from `createGeneration` does not know about the freshly-created `Quiz` (StudyQuestionService.java:51-65). The current workaround re-fetches the quiz and stitches it onto the DTO via the placeholder variable `argh`. Cleaner: set `generation.setQuiz(quiz)` in-memory before mapping (or refresh the entity), then a single `generationMapper.toDto(generation)` suffices.

### Code hygiene before opening the PR

- [ ] Placeholder variable name `argh` in committed code (StudyQuestionService.java:63). Rename.
- [ ] Commented-out debug print statements left in StudyQuestionService (lines 59-61). Remove.
- [ ] Commented-out scaffolding in CollectionController (`//GenerationDto generationdto = studyQuestionService.generateStudyQuestions(request);` at line 125). Remove.
- [ ] Stale `//fixme should have content` on `createGeneration` return (CollectionController.java:128) — `response` *is* the content. Remove the comment.
- [ ] Three unresolved `//fixme ide reports xss risk in method` comments without investigation (UserController.java:29, CollectionController.java:50, CollectionController.java:89). Either remediate or document why the warning is a false positive.
- [ ] Step-by-step scaffolding comments left in `CollectionController.createGeneration` (lines 122-127, "step 1 / step 2 / step 3"). Remove now that the method is implemented.
- [ ] Swedish comments in production code that the "translation" commit (`b6c2df9`) was supposed to clean up: StudyQuestionService.java:29,36-37,41,44,47,50,67; MockLlmService.java:6; GenerationMapper.java:16; OllamaLlmService.java throughout; SessionContext.java:11.
- [ ] `SettingsService.allSettings` output formatting glitches (SettingsService.java):
  - line 24: `"Focus area =  "` has a double space.
  - line 20: prefixes value with `"Language: "` even though the section header `"LANGUAGE:"` already labels it (redundant).
  - Casing/style is inconsistent across sections: lowercase difficulty tokens vs. `"true/false"` vs. `"multiple choice"`. Worth a quick unit test that pins the rendered prompt fragment.
- [ ] `application.properties:66` ships with `spring.profiles.active=mock` and a comment `# fixme should be ollama in prod`. Decide before merge: either leave `mock` as the default and let prod override via env/profile, or flip the default. Either way, address the fixme.
- [ ] Dead method `deleteSourceMaterial` (CollectionServiceTest.java:230-235) — a service-style method accidentally pasted into the test class. Not annotated `@Test`, but it does not belong here. Remove.
- [ ] Test name `createGeneration_returns201_withEmptySourceMaterials` (CollectionControllerTest.java:239) is misleading — the body passes a non-empty `material.getId()` and asserts on the mock LLM's Swedish return string. Rename to reflect what it actually exercises.
- [ ] OllamaLlmServiceTest disabled with two alternative `@Disabled*` strategies coexisting as comments plus a `//llm-suggested alternative^` annotation (OllamaLlmServiceTest.java:61-63). Pick one and delete the other.

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