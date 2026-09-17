# Local Development Infrastructure

**Status: infrastructure foundation (P1-T03).** This document explains how
to run ShopSphere's local infrastructure dependencies (PostgreSQL, Redis,
RabbitMQ) via Docker Compose. **No application services (ShopSphere
backend/frontend, AI AMS) are started by this Compose file** — it provides
infrastructure only, for use by future implementation tickets.

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
infrastructure only. It does not create any database schema, RabbitMQ
exchanges/queues/bindings, or Redis usage patterns — those are defined by
the ShopSphere and AI AMS services in later implementation tickets, per
[`docs/architecture/ai-ams-architecture.md`](ai-ams-architecture.md) and
[ADR-002](../adr/ADR-002-event-driven-architecture.md).
