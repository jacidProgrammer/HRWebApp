# HRWebApp — Spring Boot Clean Architecture (AI‑Assisted, Product‑Driven)

HRWebApp is a backend API built with Java and Spring Boot using Clean Architecture. 

- Language: Java 21
- Build: Maven (JAR)
- Spring Boot: 3.5.x
- Purpose: Enterprise SaaS foundations for HR/ERP features with clean boundaries, fast iteration, and high quality.

Quick links:
- OpenAPI UI (dev): /swagger-ui.html → /swagger-ui/index.html
- Docs JSON: /v3/api-docs

---

# Architecture Decisions and Rationale

This document explains the architectural choices made for HRWebApp powered by AI-assisted workflows.

## Why Clean Architecture

- Robustness and adaptability: Clean Architecture enforces strict dependency rules (outer layers depend on inner ones), isolating business logic from frameworks and infrastructure. This makes the system resilient to change (e.g., swapping Express for Spring MVC, JPA for another persistence mechanism, PostgreSQL for MongoDB) with minimal code impact.
- Fast iteration without sacrificing quality: By separating domain and application use cases from adapters and infrastructure, we can prototype features rapidly while keeping core business rules stable and well-tested.
- Testability: Inner layers (domain, application) can be unit-tested without booting Spring or a database. Adapters are covered by integration tests, with Testcontainers when needed.

## Scope and Focus (Backend-first)

- I am capable of delivering frontend (React/TypeScript) components; however, given limited time, I prioritized backend architecture and implementation to ensure a robust foundation.
- With more time, I would provide a minimal, functional UX for key flows, aligning the API contracts and frontend state management, and add API reference samples to the UI.

## Data Stores and Performance

- Primary: PostgreSQL for transactional data.
- Future additions:
    - MongoDB: for logs or semi-structured data (events, audit records, or document-based features).
    - Caching: Introduce a cache layer for recurrent API calls (e.g., Redis) to reduce latency and database load.
    - Read models: If needed, introduce CQRS patterns to optimize read-heavy endpoints.
    
## How This Project Aligns With The Role

I operate in an AI‑assisted development environment. This project is structured so you can prototype quickly, integrate end‑to‑end, and keep quality high.

Core responsibilities mapped to this repo:
- Rapid prototyping:
    - Start with H2 in-memory or Testcontainers; switch to PostgreSQL for productization.
- Full‑stack implementation:
    - Backend: Spring Boot (REST), JPA, validation, OAuth2 Resource Server.
    - Data: PostgreSQL (primary), optional MongoDB module for document use cases.
    - Frontend: React/TypeScript (separate repo or sibling folder); this backend exposes clean APIs.
- Code quality & reviews:
    - Unit tests (JUnit 5), integration tests (MockMvc, Testcontainers).
    - JaCoCo coverage reports. MapStruct for deterministic mapping.
- Continuous improvement:
    - Propose better libraries, CI/CD improvements, or new AI tools.
    - Optimize build time, test feedback loops, and developer ergonomics.

---

## Architecture (Clean Architecture)

Layers:
- Domain: Pure business logic (entities, value objects, domain services).
- Application: Use cases and ports (in/out), no framework dependencies.
- Adapters (Interface Adapters): HTTP controllers, presenters, mappers; persistence adapters with JPA; external clients.
- Infrastructure: Spring configuration (security, logging, OpenAPI, DI, profiles).

Dependency flow:
```
adapters -> application -> domain
infrastructure -> adapters/application -> domain
delivery -> infrastructure/adapters
```
Rule: inner layers must not import outer-layer classes.

---

## Tech Stack

Backend:
- Spring Boot: Web, Validation, Security, OAuth2 Resource Server, Data JPA
- DB: PostgreSQL (primary), H2 (dev), optional MongoDB module
- Mapping: MapStruct (1.5.x)
- Utilities: Lombok (optional)

Testing & Quality:
- JUnit 5, Spring Boot Starter Test, Spring REST Docs (MockMvc), Testcontainers (PostgreSQL)
- JaCoCo coverage (verify phase)

AI‑assisted development:
- Pair‑programmers and agents (e.g., GitHub Copilot, ChatGPT)
- Human review for business rules, security, and performance

DevOps:
- Docker for local DBs and Keycloak

# hrApplication Database Configuration Guide

This hrApplication application supports multiple databases with easy switching between H2 (in-memory) and PostgreSQL. The same repository interfaces work with both databases without any code changes.

## 🗄️ Supported Databases

### H2 Database (Default)
- **Type**: In-memory database
- **Best for**: Development, testing, quick demos
- **Data**: Resets on application restart
- **Web Console**: Available at `http://localhost:8080/h2-console`

### PostgreSQL
- **Type**: Production-grade relational database
- **Best for**: Production-like testing, persistent data
- **Data**: Persists between application restarts
- **Admin Tool**: PgAdmin available via Docker

## 🚀 Quick Start

### Docker Compose

If you have a docker-compose.yml at the repository root, you can spin up the stack with:

```bash
# Start all services defined in docker-compose.yml (in detached mode)
docker compose up -d

# Rebuild images and start (useful after code changes affecting Dockerfiles)
docker compose up -d --build

# Stop and remove containers, networks, and volumes created by up
docker compose down
```

### Option 1: H2 Database (Default)
```bash
# Run with H2 (default profile)
mvn spring-boot:run

# Or explicitly specify H2 profile
mvn spring-boot:run -Dspring.profiles.active=h2
```

### Option 2: PostgreSQL Database
```bash
# 1. Start PostgreSQL with Docker
docker-compose up -d postgres

# 2. Run application with PostgreSQL profile
mvn spring-boot:run -Dspring.profiles.active=postgres
```

## 📊 Sample Data

Both databases are automatically populated with sample data:

### H2 Sample Data (`import-h2.sql`)
- 2 employees
- 2 feedback entries

### PostgreSQL Sample Data (`import-postgres.sql`)
- 2 employees
- 2 feedback entries

## Using the API with Postman

### 1. Import the Postman collection

1. Open Postman.
2. Go to `File` \> `Import`.
3. Select the collection file located at `src/main/resources/HR.postman_collection.json`.
4. Postman will create a collection named `HR` with all the predefined requests.

### 2. Obtain an access token

1. In the `HR` collection, locate and open the `Token` request.
2. Make sure the request URL is:
    - `http://localhost:8082/realms/hr-realm/protocol/openid-connect/token`
3. Click `Send`.
4. In the response body, copy the value of the `access_token` field.

### 3. Use the token in the secured requests

1. Open any secured request in the `HR` collection, for example:
    - `Employees`
    - `Employees By Name`
    - `Feedback`
    - `Send feedback`
2. Go to the `Authorization` tab.
3. Select `Bearer Token` as the auth type.
4. Paste the `access_token` you copied in the `Token` field.
5. Click `Send` to call the API with authentication.

## 📈 Production Recommendations

### For Development
- Use **H2** for quick testing and demos
- No setup required, immediate startup

### For Integration Testing
- Use **PostgreSQL** with Docker
- Closer to production environment
- Persistent data for longer test cycles

### For Production
- Use external PostgreSQL instance
- Update `application-postgres.properties` with production credentials
- Consider using environment variables for sensitive data: