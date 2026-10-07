# P1-T05 - Spring Boot Core

**Commit:** `c38e1d5` - `feat: establish spring boot core backend`

## Delivered

- Dependencies: `spring-boot-starter-web`, `-actuator`, `-data-jpa`
  (replacing `-jdbc`). PostgreSQL driver and Flyway unchanged.
- `GET /api/v1/health` -> `200 {"status":"UP"}` (`HealthController`).
- `GlobalExceptionHandler` + `ApiError`: every error returns
  `timestamp`, `status`, `error`, `message`, `path`. Unexpected exceptions
  return a generic 500 message and are logged server-side.
- Actuator: only `health` exposed, details hidden (`/actuator/health`).
- JPA: `ddl-auto=validate` (Flyway owns the schema),
  `open-in-view=false`. No entities.
- `SERVER_PORT` (default 8080).
- Tests: context startup (needs the Docker PostgreSQL), health endpoint,
  500 and 404 error shape.

## Verify

```bash
docker compose up -d
cd backend/shopsphere-api
./mvnw clean verify
./mvnw spring-boot:run
curl localhost:8080/api/v1/health
curl localhost:8080/actuator/health
```

Result when delivered: build and 4 tests passed; app started twice with
Flyway applying V1 only once.

## Notes

- Existing package `com.shopsphere.api` and `application.properties` kept.
  Only `controller` and `exception` packages exist so far.

## Out of scope

Security (P1-T06), Redis, RabbitMQ, business domains, AI AMS, frontend,
observability beyond health.
