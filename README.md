# Taskflow API

## Project overview

Taskflow API is a Spring Boot REST API for managing tasks and their ordered steps. It provides task and step CRUD, task completion, keyword and status filtering, pending-task due-date queries, validation, centralized error handling, JPA persistence to H2, and AI-generated task summaries. Integration tests exercise the application through MockMvc.

## Technology stack

| Technology | Version or source |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring AI | 2.0.1 |
| Maven Wrapper | Maven 3.10.0 |
| Spring Web MVC, Data JPA, Validation, H2 | Managed by Spring Boot |
| JUnit Jupiter, MockMvc, AssertJ | Spring Boot test dependencies |

Lombok generates entity accessors and constructors.

## Architecture

```text
HTTP client
    |
    v
TaskController
    |
    v
TaskService
    +--> TaskServiceImpl (`jpa`) --> repositories --> H2
    +--> TaskServiceCollectionImp (`memory`) --> concurrent maps
    +--> Spring AI ChatClient --> OpenAI
```

The controller exposes the HTTP API and delegates to `TaskService`. Spring profiles select the JPA or collection-backed implementation; `jpa` is the configured default.

## Domain model

### Task

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | `Long` | Generated identifier |
| `title` | `String` | Required, nonblank title |
| `note` | `String` | Optional notes |
| `completed` | `boolean` | Completion state |
| `dueDate` | `LocalDate` | Optional date; validated API requests require today or later |
| `steps` | `List<Step>` | Child steps with cascading persistence |

### Step

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | `Long` | Generated identifier |
| `stepOrder` | `Integer` | Server-assigned order within a task |
| `description` | `String` | Required, nonblank instruction |
| `task` | `Task` | Required owning task; omitted from step JSON |

The relationship is `Task 1 ---- * Step`. Nested step endpoints return steps in ascending `stepOrder`.

## API endpoints

| Method | Endpoint | Description | Success |
| --- | --- | --- | --- |
| `GET` | `/api/tasks` | List all tasks | `200` |
| `GET` | `/api/tasks/{id}` | Get a task | `200` |
| `POST` | `/api/tasks` | Create a task | `201` |
| `PUT` | `/api/tasks/{id}` | Replace editable fields | `200` |
| `DELETE` | `/api/tasks/{id}` | Delete a task and its steps | `204` |
| `PUT` | `/api/tasks/{id}/complete` | Mark a task completed | `200` |
| `GET` | `/api/tasks/summary` | Return a plain-text AI summary | `200` |
| `GET` | `/api/tasks/search?keyword={value}` | Search title or note, ignoring case | `200` |
| `GET` | `/api/tasks/completed?completed={boolean}` | Filter by completion state | `200` |
| `GET` | `/api/tasks/due-before?dueDate={yyyy-MM-dd}` | List pending tasks strictly before a date, ordered by date | `200` |
| `POST` | `/api/tasks/{taskId}/steps` | Add the next ordered step | `201` |
| `GET` | `/api/tasks/{taskId}/steps` | List ordered steps | `200` |
| `GET` | `/api/tasks/{taskId}/steps/{stepId}` | Get a task's step | `200` |
| `PUT` | `/api/tasks/{taskId}/steps/{stepId}` | Update a step description | `200` |
| `DELETE` | `/api/tasks/{taskId}/steps/{stepId}` | Delete a step | `204` |

There is no dedicated reopen endpoint. `PUT /api/tasks/{id}` can set `completed` to `false`.

## Example requests

Create a task:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Complete Spring AI assignment",
    "note": "Finish the integration tests",
    "completed": false,
    "dueDate": "2026-10-30"
  }'
```

Add a step:

```bash
curl -X POST http://localhost:8080/api/tasks/1/steps \
  -H 'Content-Type: application/json' \
  -d '{"description":"Run the full test suite"}'
```

Find pending tasks due before a date:

```bash
curl 'http://localhost:8080/api/tasks/due-before?dueDate=2026-11-01'
```

Task IDs, step IDs, step order, and the owning task are assigned by the service.

## Spring AI summary

`GET /api/tasks/summary` retrieves all tasks and formats their titles, notes, completion states, and due dates for a Spring AI `ChatClient` prompt. Completed tasks are labeled `DONE`; incomplete tasks before today are `OVERDUE`; remaining tasks are `PENDING`. The configured OpenAI model returns a concise summary and prioritization.

An empty task list returns `There are no tasks.` without calling the model. Model output is nondeterministic.

## Configuration

The main configuration uses the `jpa` profile, `jdbc:h2:mem:taskflow-api`, the H2 console at `/h2`, and OpenAI model `gpt-4o-mini` with temperature `0.7`.

Tests use `jdbc:h2:mem:taskflow-integration-test;DB_CLOSE_DELAY=-1`. The suffix keeps the database available for the test JVM after the last connection closes. Tests use schema `create-drop`, disable Open Session in View, and disable persistence lifecycle validation so historical overdue fixtures can be inserted directly; controller validation remains enabled.

Both H2 databases are in memory and disappear when their JVM stops.

### `OPENAI_API_KEY`

Spring AI reads its credential from `OPENAI_API_KEY`. Never commit the key.

```bash
# Linux/macOS
export OPENAI_API_KEY="your-key"
```

```powershell
# PowerShell
$env:OPENAI_API_KEY="your-key"
```

The automated summary test replaces `ChatClient` with a deterministic test bean and does not call OpenAI. The variable must still be defined when the current application or test context starts because configuration resolves it during startup; a non-secret placeholder is sufficient for tests.

## Running the application

```bash
export OPENAI_API_KEY="your-key"
./mvnw clean spring-boot:run
```

The API is at `http://localhost:8080`. The H2 console is at `http://localhost:8080/h2`; use JDBC URL `jdbc:h2:mem:taskflow-api`, user `sa`, and a blank password.

Select the collection-backed implementation with:

```bash
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=memory
```

## Running the tests

```bash
export OPENAI_API_KEY="test-placeholder"
./mvnw test
```

The integration tests use `@SpringBootTest`, MockMvc, the real application context, the `test` and `jpa` profiles, transactions, and an in-memory H2 test database. They cover application startup, successful task and step CRUD, database persistence, task completion, cascade deletion, searches, due-date ordering, validation failures (`400`), missing resources (`404`), nested-resource ownership, and deterministic summary behavior. Docker and PostgreSQL are not required.

### Test output

Integration tests print `Response Code: ...` and `Response Body: ...` for API calls. Maven Surefire writes XML and text reports to `target/surefire-reports/`; generated `target/` content should generally not be committed.

## Error handling

`GlobalExceptionHandler` is a centralized `@RestControllerAdvice`. Responses contain `message` and `timestamp`.

| Condition | Status |
| --- | --- |
| `TaskNotFoundException` | `404 Not Found` |
| `StepNotFoundException` | `404 Not Found` |
| Request-body validation failure | `400 Bad Request` |
| Other uncaught exception | `500 Internal Server Error` with a generic message |

## Validation

- Task titles must contain non-whitespace text (`@NotBlank`).
- Task due dates must be today or later (`@FutureOrPresent`).
- Step descriptions must contain non-whitespace text (`@NotBlank`).

`@Valid` applies these rules to task create/update and step create/update requests.

