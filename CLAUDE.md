# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Eduo is a Spring Boot REST API backend paired with a React/Vite frontend. Teachers upload learning materials (PDFs, text) and request AI-powered study quiz generation via an LLM integration. The backend manages projects, materials, generations, and quizzes; the frontend renders the quiz UI.

**Stack:** Spring Boot 4.0.6, Java 25, PostgreSQL, Spring Data JPA/Hibernate, MapStruct 1.6.3, Lombok, JUnit 5, Testcontainers

---

## Code review process

Use `git --no-pager log --oneline` to list commits, then `git --no-pager show
<hash>` for each diff. The `--no-pager` flag bypasses the delta pager and gives
plain unified diff text. Fetch all hashes in parallel once the log is known,
then review oldest-to-newest. At the end of every review, read `TODO.md` and
update it to reflect any newly found issues and to close off anything that was
fixed.

---

## Commands

```bash
# Build
./mvnw clean package

# Run (requires PostgreSQL)
./mvnw spring-boot:run

# Run all tests (Testcontainers spins up PostgreSQL automatically)
./mvnw test

# Run a single test class
./mvnw test -Dtest=ProjectServiceTest

# Run a single test method
./mvnw test -Dtest=ProjectServiceTest#createProject_persistsProjectWithGeneratedId

# Skip tests
./mvnw clean package -DskipTests
```

Frontend (in `eduo-frontend/`):
```bash
npm install && npm run dev
```

---

## Database Setup

Default dev config (`application.properties`):
```
spring.datasource.url=jdbc:postgresql://localhost:5432/eduo-dev-cult
spring.datasource.username=postgres
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

- Schema name: `eduo` (Hibernate auto-creates/updates in dev mode)
- `data.sql` seeds sample data on startup (`spring.sql.init.mode=always`)
- Tests use Testcontainers with `application-test.properties` (`ddl-auto=create-drop`, `init.mode=never`)
- `TestContainersInitializer` is a Spring `ApplicationContextInitializer` that injects the container's JDBC URL before the Spring context starts — used in `@ContextConfiguration(initializers = TestContainersInitializer.class)`

---

## Architecture

### Domain Model

```
User (Integer PK)
├── UserCredential (username/password, 1:1, cascade delete)
├── UserPreferences (1:1, cascade delete)
└── Project (UUID PK, cascade delete)
    ├── SourceMaterial[] (file bytes stored as BYTEA in DB, cascade delete)
    └── Generation[] (one LLM call = one Generation, cascade delete)
        ├── GenerationSourceMaterial[] (join table: which materials fed this generation)
        └── Quiz (raw LLM output as String, cascade delete)
```

- **Project** is the core aggregate — deleting it cascades to all children
- **Quiz** stores AI output as an unstructured `rawContent` String (no JSON schema enforcement)
- **Generation** records are immutable after creation; only Quiz is editable via PATCH

### Layers

**Controllers** (`/controller`)
- `ProjectController` — CRUD for Projects, SourceMaterials, Generations, Quizzes
- `UserController` — register, login, get, delete
- `GlobalExceptionHandler` — maps exceptions to HTTP status codes

**Services** (`/service`)
- `ProjectService` — all project-domain writes are `@Transactional`; throws `EntityNotFoundException` for missing IDs (no silent no-ops, except `deleteSourceMaterial` which silently succeeds — be aware of this inconsistency)
- `AuthService` — user lifecycle; `createUser` checks username uniqueness; `loginUser` updates `lastLoginAt`
- `LlmService` (interface) — strategy pattern; `MockLlmService` is the only impl (placeholder for real LLM)

**Repositories** (`/repository`) — Spring Data `CrudRepository` interfaces, no custom queries

**DTOs & Mappers** (`/dto`, `/mapper`) — MapStruct compile-time mappers; Java records for request bodies

### Key Patterns

- **Cascade deletes** enforce ownership: controllers only need to delete the root entity
- **`@Transactional(readOnly=true)`** on read methods in services (Hibernate optimization hint)
- **UUID** for Project/Generation/Quiz IDs; **Integer** auto-increment for User IDs
- **`SessionContext`** bean is a temporary in-memory holder for current user — marked for replacement with proper auth (JWT/session)
- **LlmService** interface makes swapping providers trivial — implement and switch the `@Bean` via `@Profile`

### Testing Pattern

Service and controller tests use `@SpringBootTest` + real PostgreSQL via Testcontainers. Tests are `@Transactional` for auto-rollback. Pattern:

```java
@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitializer.class)
@Transactional
class ProjectServiceTest {
    // Call entityManager.flush() + entityManager.clear() to force DB round-trip
    // Use helper methods like persistUser() to satisfy FK constraints
}
```

---

## Git Workflow

- `main` — production-ready, 2+ approvals, merged from `dev` weekly
- `dev` — integration branch, 1+ approval required
- `feature/{issue}` — feature branches based on `dev`, PR back to `dev`