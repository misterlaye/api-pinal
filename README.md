# api-pinal

Spring Boot modular monolith for dairy farm management.

## Requirements

- Java 25
- Docker Compose

## Local configuration

Create a `.env` file in the project root with `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`, and a `JWT_SECRET` of at least 32 characters. Set
`DATABASE_URL` only when PostgreSQL is not available at the default local URL.
The `.env` file is ignored by Git and must not be committed.

Start PostgreSQL and the application on Windows:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Run the test suite with:

```powershell
.\mvnw.cmd test
```
