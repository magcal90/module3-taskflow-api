# Taskflow API

A Spring Boot REST API for managing tasks and generating an AI-written summary of the task list.

## Requirements

- Java 21
- An OpenAI API key to use the task summary endpoint when tasks exist

The project includes the Maven Wrapper, so a separate Maven installation is not required.

## Run locally

Set `OPENAI_API_KEY` in your environment, then start the application:

```bash
export OPENAI_API_KEY="your-api-key"
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`. The OpenAI chat model is configured as `gpt-4o-mini` in `src/main/resources/application.properties`.

Task data is stored in memory and is cleared when the application restarts.

## API

All task endpoints use the `/api/tasks` base path. Task JSON has this shape:

```json
{
  "id": 1,
  "title": "Finish assignment",
  "description": "Implement the summary endpoint",
  "completed": false,
  "dueAt": "2026-10-21T17:00:00"
}
```

`id` is assigned by the API when creating a task. `dueAt` is an ISO-8601 local date-time, or `null` if there is no due date.

| Method | Path | Description | Success response |
| --- | --- | --- | --- |
| `GET` | `/api/tasks` | List all tasks | `200 OK` |
| `GET` | `/api/tasks/{id}` | Get a task by ID | `200 OK` |
| `POST` | `/api/tasks` | Create a task | `201 Created` |
| `PUT` | `/api/tasks/{id}` | Replace a task's fields | `200 OK` |
| `DELETE` | `/api/tasks/{id}` | Delete a task | `204 No Content` |
| `PUT` | `/api/tasks/{id}/complete` | Mark a task complete | `200 OK` |
| `GET` | `/api/tasks/summary` | Generate a plain-text summary | `200 OK` |

An unknown task ID returns `404 Not Found`. The summary endpoint returns `There are no tasks.` when the list is empty; otherwise it calls OpenAI and requires `OPENAI_API_KEY`.

### Examples

Create a task:

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Finish assignment",
    "description": "Implement the summary endpoint",
    "completed": false,
    "dueAt": "2026-10-21T17:00:00"
  }'
```

List tasks:

```bash
curl http://localhost:8080/api/tasks
```

Mark task `1` complete:

```bash
curl -X PUT http://localhost:8080/api/tasks/1/complete
```

Get an AI summary:

```bash
curl http://localhost:8080/api/tasks/summary
```

## Build and test

```bash
./mvnw test
./mvnw package
```