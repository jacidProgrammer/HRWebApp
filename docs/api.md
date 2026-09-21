# API reference

A human-oriented summary of the HTTP API. The contract is [`openapi.json`](openapi.json); back to the [README](../README.md).

All endpoints require `Authorization: Bearer <token>`. Ids are UUIDs, dates are ISO-8601 UTC strings
(`2026-09-21T10:15:30Z`).

## Employees

```json
{ "id": "uuid", "username": "jose", "name": "José Antonio Cid", "department": "IT", "role": "Java Senior Backend",
  "email": "jose@example.com", "salary": 75600.0, "address": "Mainz, Germany", "createdAt": "2025-08-17T19:20:12Z" }
```

| Method | Path               | Role                  | Description                                                                 |
|--------|--------------------|-----------------------|-----------------------------------------------------------------------------|
| GET    | `/employees`       | `MANAGER`, `EMPLOYEE` | All employees; `salary`/`address` are `null` unless manager or own record   |
| GET    | `/employees/me`    | `MANAGER`, `EMPLOYEE` | The employee linked to the caller's username; `404` if there is none        |
| GET    | `/employees/{id}`  | `MANAGER`, `EMPLOYEE` | One employee (same visibility rule)                                         |
| POST   | `/employees`       | `MANAGER`             | Create: `username, name, department, role, email, salary, address` (all required); `201` |
| PUT    | `/employees/{id}`  | `MANAGER`, `EMPLOYEE` | Update (rules below)                                                        |
| DELETE | `/employees/{id}`  | `MANAGER`             | Delete; `204`                                                               |

- `username` is unique (case-insensitive, stored in lower case), must look like a Keycloak username
  (letters, digits, `. _ @ -`) and never changes.
- A manager replaces every field except `username`, so the body must contain all of them. A `username` in the
  body is only accepted if it is the current one (otherwise `400`).
- An employee can only update their own record and only `email` and `address` (omitted values are kept).
  Other fields may be omitted or sent unchanged; changing them returns `403`, as does updating someone else.
- **Deleting an employee** deletes the feedback **about** them and keeps the feedback they **wrote** with
  `authorId`/`authorName` = `null`, so their colleagues keep what they received.

## Feedback

```json
{ "id": "uuid", "recipientId": "uuid", "recipientName": "Louisa Becker", "authorId": "uuid", "authorName": "José Antonio Cid",
  "anonymous": false, "value": "TEAMWORK", "message": "Great facilitation!",
  "sentiment": { "label": "POSITIVE", "score": 0.97 }, "createdAt": "2026-09-21T10:15:30Z" }
```

| Method | Path                 | Role       | Description                                                                          |
|--------|----------------------|------------|--------------------------------------------------------------------------------------|
| POST   | `/feedback`          | `EMPLOYEE` | Send `{ "recipientId", "message", "value"?, "anonymous"? }`; `201`                   |
| GET    | `/feedback/received` | `EMPLOYEE` | Feedback about the caller, newest first                                              |
| GET    | `/feedback/sent`     | `EMPLOYEE` | Feedback written by the caller, newest first (author always filled in)               |
| GET    | `/feedback`          | `MANAGER`  | All feedback, newest first. Optional filters below                                   |

- The caller of `POST /feedback`, `/received` and `/sent` needs an employee record (`403` otherwise).
  `message` must have 1 to 500 characters (surrounding whitespace is removed), `value` is optional,
  `anonymous` defaults to `false`. Feedback to yourself is `400`, an unknown recipient `404`.
- `authorId`/`authorName` are `null` for anonymous feedback, for everybody including managers. The only
  exception is the author: `/feedback/sent` and the `POST` response show it.
- `sentiment` is `null` when the analysis is disabled, failed, or no token is configured.
- Filters of `GET /feedback`: `recipientId`, `department` (case-insensitive), `from` and `to` (inclusive; a date
  such as `2026-09-01` means the whole UTC day, or a date-time such as `2026-09-01T10:15:30Z`), `sentiment`
  (`POSITIVE`, `NEUTRAL`, `NEGATIVE`, or `NONE` for feedback without sentiment).

## Dashboard and settings

| Method | Path              | Role                  | Description                                                                 |
|--------|-------------------|-----------------------|-----------------------------------------------------------------------------|
| GET    | `/stats/overview` | `MANAGER`             | Dashboard figures; optional `months` (default 6, 1..12)                     |
| GET    | `/settings`       | `MANAGER`, `EMPLOYEE` | `{ "sentimentAnalysisEnabled": true, "sentimentAnalysisAvailable": false }` |
| PUT    | `/settings`       | `MANAGER`             | `{ "sentimentAnalysisEnabled": false }`, returns the settings               |

`GET /stats/overview` (all months are UTC calendar months):

| Field            | Meaning                                                                                                   |
|------------------|-----------------------------------------------------------------------------------------------------------|
| `headcount`, `departments` | Number of employees, in total and per department                                                |
| `feedback`       | Feedback created this month, last month, and in total                                                     |
| `sentimentShare` | Share (0..1, two decimals) of positive / neutral / negative / not analysed feedback in the selected `months` |
| `trend`          | One entry per month of the selected period, oldest first, zero-filled                                     |
| `valueCounts`    | Every company value with the number of feedback items recognising it in the selected period, most frequent first |
| `topRecognised`  | The 5 employees with most feedback received in the last 90 days, with their positive share (positive / analysed) |
| `alerts`         | Employees whose positive share in the last 30 days dropped by ≥ 0.25 compared with the 30 days before, with ≥ 3 analysed items in each window. `feedbackCount` is the number of items in the last 30 days. No message content |

## Errors

Errors produced by the API have the body `{"code": "...", "message": "..."}`. `401` responses and `403` responses
caused by the caller's role come from Spring Security, with no body (`401` carries `WWW-Authenticate: Bearer`).

| Status | When                                                                                          |
|--------|-----------------------------------------------------------------------------------------------|
| 400    | Missing required fields, changing a username, invalid feedback (message length, feedback to yourself, unknown value), malformed ids, JSON or filters, `months` outside 1..12 |
| 401    | Missing or invalid token                                                                      |
| 403    | Role not allowed for the endpoint, updating another employee or a manager-only field, sending/reading feedback without an employee record |
| 404    | Employee not found, `/employees/me` without employee record                                   |
| 409    | Creating an employee whose username already exists                                            |

Every response, errors included, carries a correlation id in the `X-Request-Id` header; quote it when reporting a problem.

## Machine-readable contract

The authoritative description is the OpenAPI document [`openapi.json`](openapi.json), generated from the code and
checked in CI ([ADR 0006](adr/0006-openapi-contract-between-repos.md)). With the application running, the same
document is served at `http://localhost:8080/v3/api-docs` and explorable in Swagger UI at
`http://localhost:8080/swagger-ui.html` (use **Authorize** with an access token).
