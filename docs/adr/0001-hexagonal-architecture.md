# 0001. Hexagonal architecture (ports and adapters)

- Status: Accepted
- Date: 2026-09-21

## Context and problem statement

The first version grew as a classic layered Spring application: controllers, services, JPA entities and
the Hugging Face client referenced each other directly, and the web DTOs doubled as the domain model. Business
rules (who may see a salary, who may edit which field, when an alert is raised) were hard to test without
starting Spring, and swapping the external sentiment service or the database meant touching the services.

How should the code be organised so that the rules are testable in isolation and the integrations are replaceable?

## Considered options

- Layered architecture (controller → service → repository), as before
- Hexagonal architecture: domain + application core with inbound and outbound ports, adapters around it
- Package-by-feature without explicit ports

## Decision outcome

Chosen option: **hexagonal architecture**, with these packages:

- `domain`: plain Java records and policies (`Employee`, `Feedback`, `EmployeeUpdatePolicy`, `StatsCalculator`), no Spring, no I/O;
- `application.port.in`: use case interfaces the HTTP adapter calls; `application.port.out`: what the core needs
  from the outside (`EmployeeRepository`, `SentimentAnalyzer`, `CurrentUserProvider`, `TimeProvider`, `FeedbackMetrics`);
- `application.services`: use case implementations that only see ports and the domain;
- `adapter.in.http`, `adapter.out.persistence`, `adapter.out.ai`, `adapter.out.metrics`: DTOs, JPA entities and HTTP
  clients stay here and are mapped with MapStruct;
- `infrastructure`: Spring wiring, security, clock, seeding.

### Consequences

- Good: the domain rules and services are covered by fast unit tests with fakes and a fixed clock; the JSON contract,
  the schema and the external APIs can change without touching the use cases.
- Good: new integrations (a different sentiment model, metrics) are one adapter implementing an existing or new port.
- Bad: more types and mapping code than a layered CRUD application of this size strictly needs.
- Neutral: the boundaries are enforced by convention and code review; there is no ArchUnit test yet.
