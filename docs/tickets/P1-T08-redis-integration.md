# P1-T08 - Redis Integration

**Commit:** `feat: establish redis caching foundation`

## Delivered

- Dependencies: `spring-boot-starter-data-redis`, `spring-boot-starter-cache`.
- `cache` package: Redis `CacheManager` (default TTL 10m, per-cache TTL via
  `app.cache.ttls.*`, `demo` = 5m), JSON value serialization with a
  restricted type validator, string keys, no null caching.
- Degraded mode: cache errors are logged and skipped; the app starts and
  serves without Redis (no retries / circuit breakers).
- `CacheDemoService` demonstrating direct store/retrieve/remove and
  `@Cacheable` / `@CachePut` / `@CacheEvict`. No endpoint; not tied to any
  business logic.
- Config: `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` (+ 2s timeouts)
  in `application.properties` and `.env.example`.
- Docs: `docs/architecture/redis-caching.md`; pointer from
  `local-development.md`.

## Docker Compose

Unchanged. The `redis:7-alpine` service from P1-T03 already has a
healthcheck, `restart: unless-stopped`, `REDIS_PORT` and a volume.

## Verify

See `docs/architecture/redis-caching.md`.

Result when delivered: 34 tests passed (9 Redis integration, 1 degraded mode,
plus the 24 existing). App verified starting with Redis up and with Redis
unreachable.

## Notes

- Tests use the Compose Redis (consistent with the PostgreSQL test); no
  Testcontainers.
- `/actuator/health` goes DOWN (503) while Redis is unreachable; it is kept
  visible deliberately. See the doc for how to change it.
- The app was not run inside Docker; the `redis` hostname applies only when it
  is containerized later.

## Out of scope

Cart, product/order/inventory caching, distributed sessions, task queues,
AI agent memory, AMS integration, Redis auth/TLS.
