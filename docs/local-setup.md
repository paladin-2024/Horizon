# Local setup

Requires Java 21, PostgreSQL 17 and Node 18.18+. There is no Docker and no Maven install: use the wrapper (`./mvnw`).

## PostgreSQL 17

Install and start it with Homebrew: `brew install postgresql@17 && brew services start postgresql@17`.

The app defaults assume PostgreSQL 17 on port **5433**, because the original dev machine runs PostgreSQL 18 on 5432. If yours is on 5432, set `DB_URL` and `TEST_DB_URL` (below).

Create the two databases:

```bash
createdb -p 5433 horizon
createdb -p 5433 horizon_test
```

## Configuration

| Variable | Default | Used by |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/horizon` | backend |
| `DB_USER` | your OS user | backend |
| `DB_PASSWORD` | empty | backend |
| `TEST_DB_URL` | `jdbc:postgresql://localhost:5433/horizon_test` | backend tests |
| `API_URL` | `http://localhost:8080` | web proxy |

Secrets go in environment variables or in `backend/config/application-local.yml` (gitignored; Spring Boot reads `./config/` from the working directory, so run from `backend/`, and activate it with `SPRING_PROFILES_ACTIVE=local`). Never put secrets under `src/main/resources`: everything there is packaged into the jar.

## Run

```bash
cd backend && ./mvnw spring-boot:run  # http://localhost:8080/actuator/health
cd web && npm install && npm run dev  # http://localhost:3000, /api/* is proxied to the API
```

The API's GC log is written to `backend/target/gc.log`.

Next.js bakes `API_URL` in at build time (`next build` evaluates the rewrite), so a production build needs `API_URL` set when it is built.

If port 8080 is already in use on your machine, run `SERVER_PORT=8081 ./mvnw spring-boot:run` from `backend/` and `API_URL=http://localhost:8081 npm run dev` from `web/`. The health URL is then `http://localhost:8081/actuator/health`.

## Test

```bash
cd backend
./mvnw test                                                   # all tests
./mvnw test -Dtest=HealthEndpointTest                         # one class
./mvnw test -Dtest=HealthEndpointTest#healthIsPublicAndUp     # one method
```

Tests use the real `horizon_test` database; Hibernate recreates the schema for each run.
