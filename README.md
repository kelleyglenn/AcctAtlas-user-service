# AcctAtlas User Service

User identity and access management service for AccountabilityAtlas. Handles registration, authentication (email/password + OAuth), JWT token issuance, profile management, and trust tier progression.

## Prerequisites

- **Docker Desktop** (for PostgreSQL and Redis)
- **Git**

JDK 21 is managed automatically by the Gradle wrapper via [Foojay Toolchain](https://github.com/gradle/foojay-toolchain) -- no manual JDK installation required.

## Clone and Build

```bash
git clone <repo-url>
cd AcctAtlas-user-service
```

Build the project (downloads JDK 21 automatically on first run):

```bash
# Linux/macOS
./gradlew build

# Windows
gradlew.bat build
```

## Local Development

### Start dependencies

```bash
docker-compose up -d
```

This starts PostgreSQL 17 and Redis 7. Flyway migrations run automatically when the service starts.

### Run the service

```bash
# Linux/macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

The service starts on **http://localhost:8081**.

### Run tests

```bash
./gradlew test
```

Integration tests use [TestContainers](https://testcontainers.com/) to spin up PostgreSQL automatically -- Docker must be running.

### Code formatting

Formatting is enforced by [Spotless](https://github.com/diffplug/spotless) using Google Java Format.

```bash
# Check formatting
./gradlew spotlessCheck

# Auto-fix formatting
./gradlew spotlessApply
```

### Full quality check

Runs Spotless, Error Prone, tests, and JaCoCo coverage verification (80% minimum):

```bash
./gradlew check
```

## Docker Image

Build a Docker image locally using [Jib](https://github.com/GoogleContainerTools/jib) (no Dockerfile needed):

```bash
./gradlew jibDockerBuild
```

Build and start the full stack (service + Postgres + Redis) in Docker:

```bash
./gradlew composeUp
```

## Project Structure

```
src/main/java/com/accountabilityatlas/userservice/
  config/        Spring configuration (Security, JPA, JWT)
  domain/        JPA entities (User, Session, OAuthLink, etc.)
  repository/    Spring Data JPA repositories
  service/       Business logic
  web/           Controller implementations
  event/         Event publisher interface

src/main/resources/
  application.yml          Shared config
  application-local.yml    Local dev overrides
  db/migration/            Flyway SQL migrations

src/test/java/.../
  domain/        Entity unit tests
  service/       Service unit tests (Mockito)
  web/           Controller tests (@WebMvcTest)
  integration/   Integration tests (TestContainers)
```

API interfaces and DTOs are generated from `docs/api-specification.yaml` by the OpenAPI Generator plugin into `build/generated/`.

## Key Gradle Tasks

| Task | Description |
|------|-------------|
| `bootRun` | Run the service locally |
| `test` | Run all tests |
| `check` | Full quality gate (format + analysis + tests + coverage) |
| `spotlessApply` | Auto-fix code formatting |
| `jibDockerBuild` | Build Docker image |
| `composeUp` | Build image + docker-compose up |

## Documentation

- [Technical Overview](docs/technical.md)
- [API Specification](docs/api-specification.yaml) (OpenAPI 3.1)
- [Authentication Flows](docs/authentication-flow.md)
- [Database Schema](docs/database-schema.md)
- [Trust Tier Logic](docs/trust-tier-logic.md)
