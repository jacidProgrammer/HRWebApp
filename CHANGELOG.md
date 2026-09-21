# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). The version applies to the HTTP API contract
([`docs/openapi.json`](docs/openapi.json)) as well as to the application.

## [Unreleased]

## [2.0.0] - 2026-09-21

A recognition and insights API: breaking changes to ids, the feedback model and the endpoints, plus the tooling
to run, observe and evolve it.

### Added

- Feedback with author, optional company value (`TEAMWORK`, `OWNERSHIP`, `CRAFT`, `CUSTOMER_FOCUS`, `GROWTH`),
  optional anonymity, nested sentiment (`{label, score}`) and creation timestamp.
- `GET /feedback/received` and `GET /feedback/sent` for employees; `GET /feedback` with filters
  (`recipientId`, `department`, `from`, `to`, `sentiment`) for managers.
- `GET /employees/me`: the employee record of the caller.
- `GET /stats/overview`: headcount per department, feedback volume, sentiment share and monthly trend, recognised
  values, most recognised employees and aggregated alerts when positive feedback about someone drops.
- `GET/PUT /settings`: managers can switch the AI sentiment analysis off; nothing is sent to Hugging Face while it is off.
- Flyway migrations and a demo data seeder (12 employees, 50 feedback items over the last six months, one alert).
- Committed OpenAPI contract `docs/openapi.json` with tags, summaries, error responses, nullable fields, enums and a
  bearer JWT security scheme; `OpenApiContractTest` fails the build when it is out of date
  (`./mvnw -Pupdate-openapi test` regenerates it).
- Dockerfile (multi-stage, layered jar, non-root, health check) and a one-command full stack:
  `docker compose --profile full up -d --build` starts PostgreSQL, Keycloak, the API and the UI built from the
  HRWebApp-UI repository.
- Observability: Prometheus endpoint on a separate management port in containers, `hr_feedback_submitted_total` and
  `hr_sentiment_analysis_total` counters, HTTP latency histograms, ECS JSON logs in containers,
  `X-Request-Id` correlation id (reused or generated, returned in the response and logged via the MDC), and an
  `observability` compose profile with Prometheus and a provisioned Grafana dashboard.
- Architecture decision records in `docs/adr`.
- CodeQL analysis, Dependabot (Maven, GitHub Actions, Docker, Compose), Docker image build and a coverage badge in CI.

### Changed

- **Breaking:** employee and feedback ids are UUIDs; employees are linked to Keycloak users by `username` instead of
  their display name, and `PUT`/`DELETE` address employees by id.
- **Breaking:** the feedback request and response bodies follow the new model (`recipientId`, `message`, `value`, `anonymous`).
- Keycloak 26 realm with lowercase demo users (`manager`, `jose`, `louisa`, `maria`, `lukas`); Postman collection updated.
- The API is stateless: it no longer creates HTTP sessions.
- Error responses are documented per operation; CORS exposes `X-Request-Id` and `Location` to the browser.
- README reorganised; API reference moved to `docs/api.md`.

### Removed

- Hibernate-generated schema and the `import-*.sql` data scripts (replaced by Flyway and the seeder). A database
  created by 1.x is refused by Flyway: recreate it with `docker compose down -v`.

## [1.0.0] - 2026-09-21

First public version.

### Added

- Employee CRUD with role-based visibility: managers see and edit everything; employees see salary and address only
  on their own record and can only update their own contact details.
- Peer feedback with sentiment analysis through the Hugging Face inference API, stored without sentiment when the
  analysis fails or no token is configured.
- Hexagonal architecture: domain models, use case ports, and adapters for HTTP, JPA and the sentiment API.
- Keycloak as identity provider; the API is an OAuth2 resource server with realm roles mapped to Spring roles and
  deny-by-default authorisation.
- CORS for the HRWebApp-UI single-page app and a public PKCE client in the Keycloak realm.
- H2 (default) and PostgreSQL profiles, docker compose for Keycloak and the databases, springdoc Swagger UI.
- Unit, MockMvc and Testcontainers tests with a JaCoCo report; GitHub Actions CI; MIT license.

[Unreleased]: https://github.com/jacidProgrammer/HRWebApp/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/jacidProgrammer/HRWebApp/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/jacidProgrammer/HRWebApp/releases/tag/v1.0.0
