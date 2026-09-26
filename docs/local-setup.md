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
| `HORIZON_JWT_SECRET` | none, required | backend — base64 of 32 random bytes, signs access tokens |
| `HORIZON_ENCRYPTION_KEY` | none, required | backend — base64 of exactly 32 random bytes, AES-GCM key for the national ID column |
| `HORIZON_OTP_PEPPER` | none, required | backend — base64 of 32 random bytes, HMAC key for stored OTP hashes |
| `API_URL` | `http://localhost:8080` | web proxy |

Secrets go in environment variables or in `backend/config/application-local.yml` (gitignored; Spring Boot reads `./config/` from the working directory, so run from `backend/`, and activate it with `SPRING_PROFILES_ACTIVE=local`). Never put secrets under `src/main/resources`: everything there is packaged into the jar.

Generate the three secrets once per machine and export them before `./mvnw spring-boot:run` (the test profile carries its own fixed values, so `./mvnw test` needs none):

```bash
export HORIZON_JWT_SECRET=$(openssl rand -base64 32)
export HORIZON_ENCRYPTION_KEY=$(openssl rand -base64 32)
export HORIZON_OTP_PEPPER=$(openssl rand -base64 32)
```

Changing `HORIZON_ENCRYPTION_KEY` makes every stored national ID undecryptable; changing `HORIZON_JWT_SECRET` or `HORIZON_OTP_PEPPER` invalidates issued access tokens and pending OTPs.

Instead of exporting variables you can keep everything in `backend/config/application-local.yml` (git-ignored; run from `backend/` with `SPRING_PROFILES_ACTIVE=local`). Local development also needs `trust-forwarded-for` switched on, because the Next.js proxy is always in front of the API and every request would otherwise share the proxy's IP in the login and OTP rate limits:

```yaml
horizon:
  auth:
    jwt-secret: <base64 of 32 random bytes>
    otp-pepper: <base64 of 32 random bytes>
  security:
    encryption-key: <base64 of exactly 32 random bytes>
    trust-forwarded-for: true
```

(or export `HORIZON_SECURITY_TRUST_FORWARDED_FOR=true`). Never turn it on where the API port is reachable directly: a client could then put any address in `X-Forwarded-For`.

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
