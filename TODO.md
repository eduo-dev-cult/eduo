# TODO

## Blocked

- [ ] `downloadMaterial` — `projectId` path variable is declared but not consumed; project existence not validated. Disabled test pending decision on whether the endpoint needs to stay.
- [ ] `createGeneration` prompt building — UTF-8 decoding of all file types is incorrect for non-plaintext materials (e.g. PDFs). Known issue; endpoint is incomplete pending proper LLM integration system.

## Closed

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