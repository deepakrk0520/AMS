# Local Development Infrastructure

**Status: infrastructure foundation (P1-T03, P1-T04).** This document
explains how to run ShopSphere's local infrastructure dependencies
(PostgreSQL, Redis, RabbitMQ) via Docker Compose, and how the
`shopsphere-api` backend connects to and manages its PostgreSQL schema via
Flyway. **No business functionality (REST APIs, domain entities, Redis
caching, RabbitMQ messaging, AI AMS) is implemented here** — this covers
infrastructure and the database migration foundation only.

## Prerequisites

- Docker
- Docker Compose (the `docker compose` CLI plugin)

## Configuration

Copy the example environment file and adjust values if needed:

```bash
cp .env.example .env
```

`.env` is git-ignored and must never be committed. The defaults in
`.env.example` are local-development-only and must never be reused in a
shared or production environment.

## Starting infrastructure

```bash
docker compose up -d
```

This starts three services — `postgres`, `redis`, `rabbitmq` — all attached
to the `shopsphere-network` Docker network, with data persisted in named
volumes.

## Checking status

```bash
docker compose ps
```

All three services should reach a `healthy` state after startup (RabbitMQ
takes the longest to become ready).

## Viewing logs

```bash
docker compose logs
```

Or for an individual service:

```bash
docker compose logs postgres
docker compose logs redis
docker compose logs rabbitmq
```

## Stopping infrastructure

```bash
docker compose down
```

This stops and removes the containers but **preserves** the named volumes
(`shopsphere-postgres-data`, `shopsphere-redis-data`,
`shopsphere-rabbitmq-data`), so data survives a restart.

## Resetting infrastructure

```bash
docker compose down -v
```

This additionally removes the named volumes, **permanently deleting all
persisted data** (databases, cached keys, queued messages). Only use this
when intentionally resetting your local environment — it is not part of
normal day-to-day development and should not be run casually.

## Local endpoints (from the host machine)

| Service | Endpoint | Credentials |
|---|---|---|
| PostgreSQL | `localhost:5432` | `POSTGRES_USER` / `POSTGRES_PASSWORD` from `.env` |
| Redis | `localhost:6379` | none (local dev only) |
| RabbitMQ (AMQP) | `localhost:5672` | `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS` from `.env` |
| RabbitMQ Management UI | http://localhost:15672 | `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS` from `.env` |

These are development credentials only and must never be used in a
production environment.

## Container-to-container communication

Future application containers (ShopSphere backend, AI AMS services) that
join `shopsphere-network` must connect to these services using their
**Docker service name**, not `localhost`:

```text
postgres:5432
redis:6379
rabbitmq:5672
```

`localhost` only refers to the host machine and is only correct when
connecting from outside Docker (e.g., a local IDE or CLI tool running
directly on the developer's machine).

## Scope of this infrastructure

This Compose setup provisions PostgreSQL, Redis, and RabbitMQ as bare
infrastructure only. It does not create any RabbitMQ exchanges/queues/
bindings or Redis usage patterns — those are defined by the ShopSphere and
AI AMS services in later implementation tickets, per
[`docs/architecture/ai-ams-architecture.md`](ai-ams-architecture.md) and
[ADR-002](../adr/ADR-002-event-driven-architecture.md). The PostgreSQL
database schema itself is versioned and managed by `shopsphere-api` via
Flyway, described below.

## shopsphere-api database connection (P1-T04)

`backend/shopsphere-api` connects to the PostgreSQL container using
environment-driven configuration (`backend/shopsphere-api/src/main/resources/application.properties`):

| Variable | Purpose | Local default |
|---|---|---|
| `DB_HOST` | Database host | `localhost` (host machine) / `postgres` (once containerized) |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `shopsphere` |
| `DB_USERNAME` | Database user | `shopsphere` |
| `DB_PASSWORD` | Database password | `change-me` |

These are distinct from the `POSTGRES_*` variables above: `POSTGRES_*`
configure the container itself (via `docker-compose.yml`), while `DB_*`
configure the application's JDBC connection to that container. No
credentials are hardcoded in Java code or configuration files.

To run the API against the Dockerized PostgreSQL from the host machine:

```bash
cd backend/shopsphere-api
DB_HOST=localhost DB_PORT=5432 DB_NAME=shopsphere DB_USERNAME=shopsphere DB_PASSWORD=change-me \
  ./mvnw spring-boot:run
```

(Or export the same variables from your `.env`.)

### Flyway migrations

- **Location:** `backend/shopsphere-api/src/main/resources/db/migration/`
- **Naming convention:** `V<version>__<description>.sql` (e.g.
  `V1__initial_schema.sql`, `V2__add_products_table.sql`). Version numbers
  are sequential and never reused.
- **Execution:** Flyway runs automatically on application startup, before
  the application context finishes initializing. Pending migrations are
  applied in version order; already-applied migrations are skipped.
- **Immutability:** once a migration has been applied (recorded in
  `flyway_schema_history`), its file must never be modified. A schema
  change is always introduced as a new migration (`V2__...`, `V3__...`),
  never by editing an existing one — Flyway detects checksum mismatches on
  modified, already-applied migrations and will fail startup.

### Verifying migration status

Check applied migrations directly in PostgreSQL:

```bash
docker exec shopsphere-postgres psql -U shopsphere -d shopsphere \
  -c "SELECT version, description, success FROM flyway_schema_history;"
```

Or check the application startup logs for lines from
`org.flywaydb.core...`, which report the current schema version and any
migrations applied on that run.
