# api-pinal

Spring Boot modular monolith for dairy farm management.

## Requirements

- Java 25
- Docker Compose (PostgreSQL for local runs, Testcontainers for integration tests)

## Local configuration

Create a `.env` file in the project root with `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`, and a `JWT_SECRET` of at least 32 characters. Set
`DATABASE_URL` only when PostgreSQL is not available at the default local URL
(`jdbc:postgresql://localhost:5433/pinal_db` — note that the application database is
`pinal_db`, independently of `POSTGRES_DB`).
The `.env` file is ignored by Git and must not be committed.

`AI_COPILOT_ENABLED` must stay `false` during stabilisation (default value).

Start PostgreSQL and the application on Windows:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

### Profiles

| Profile | Effect |
|---|---|
| `dev` (default) | Simulated SMS: OTP codes are written to the application log (`[SMS simulé]`). |
| `demo` | Loads the demo farm (repeatable migration `db/demo/R__demo_dataset.sql`), only once. Combine with `dev`: `--spring.profiles.active=dev,demo`. The demo owner logs in by OTP (no PIN). |
| any other | No SMS provider is declared: the application refuses to start until a real `SmsProvider` is provided. |

The demo dataset is never part of the standard migration path.

### Database migrations

Flyway migrations live in `src/main/resources/db/migration` (plus the Java migration
`db.migration.V28__hash_existing_pins`). A database created before the stabilisation
that still references the former `V23__seed_demo_dataset` migration must be recreated
(`docker compose down -v`, after checking that the targeted volume is the local development one).

## Authentication

- Owners log in with a one-time SMS code (OTP, 5 attempts per code).
- Workers (`EMPLOYE`, `GERANT`) log in with a 6-digit PIN, given once by the owner when inviting them.
  PINs are stored as BCrypt hashes, locked for 15 minutes after 5 failures, and a PIN session never
  grants access to a farm where the user is owner.
- `/api/v1/auth/**` is rate-limited per client address (`pinal.security.auth-rate-limit.requests-per-minute`).
- Every business request is limited to the farm given by `X-Exploitation-ID`, checked against the user's memberships.

## Business rules

Milk is recorded in litres. Financial, feed-cost and dashboard indicators follow the rules
documented in [REGLES-CALCUL.md](REGLES-CALCUL.md).

## Tests

```powershell
.\mvnw.cmd test
```

runs unit tests and the Testcontainers integration tests (`*Test`, `*Tests`). End-to-end gates and
benchmarks named `*IT` are run explicitly, also on Testcontainers (never on the development database):

```powershell
.\mvnw.cmd test "-Dtest=BusinessRealityGateIT,ExploitationIsolationProofGateIT,FrontendRealityProofGateIT,AICopilotProductFlowIT,SnapshotReconstructibilityIT,DashboardLoadBenchmarkIT" "-Dsurefire.failIfNoSpecifiedTests=false"
```
