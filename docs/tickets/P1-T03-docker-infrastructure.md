# P1-T03 - Docker Local Infrastructure Foundation

**Commit:** `d2dc346` - `chore: establish local docker infrastructure`

## Delivered

- `docker-compose.yml` with three services on `shopsphere-network`, each
  with a healthcheck and a named volume:

  | Service | Image | Host port(s) |
  |---|---|---|
  | `postgres` | `postgres:16-alpine` | 5432 |
  | `redis` | `redis:7-alpine` | 6379 |
  | `rabbitmq` | `rabbitmq:3.13-management-alpine` | 5672, 15672 |

- `.env.example` (local-development defaults; `.env` is git-ignored).
- [`local-development.md`](../architecture/local-development.md): start,
  status, logs, stop, reset.

## Verify

```bash
cp .env.example .env
docker compose up -d
docker compose ps        # all three healthy
```

## Notes

- Containers reach each other by service name (`postgres:5432`); the host
  uses `localhost`.
- `docker compose down -v` deletes all persisted data.

## Out of scope

Application containers, exchanges/queues, Redis usage.
