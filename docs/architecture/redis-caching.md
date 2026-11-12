# Redis Caching (P1-T08)

## Purpose and architecture

Redis (the `redis` service in `docker-compose.yml`, `redis:7-alpine`) is a
**cache only** for `shopsphere-api`. PostgreSQL stays the source of truth, so
losing Redis never loses data. Spring Cache annotations (`@Cacheable`,
`@CachePut`, `@CacheEvict`) sit on service methods; a `RedisCacheManager`
stores the results in Redis via the Lettuce client. Nothing business-related
is cached yet: `CacheDemoService` is an isolated demonstration.

Code: `backend/shopsphere-api/src/main/java/com/shopsphere/api/cache/`

| Class | Role |
|---|---|
| `CacheConfig` | `@EnableCaching`, Redis cache manager, JSON serialization, error handler |
| `CacheProperties` | `app.cache.*` TTL settings |
| `CacheNames` | cache name constants |
| `CacheErrorLogger` | logs cache/Redis failures and continues (degraded mode) |
| `CacheDemoService`, `DemoValue` | demonstration only |

## Start Redis and connect

```bash
docker compose up -d redis
docker compose ps redis                 # STATUS: healthy
docker exec shopsphere-redis redis-cli ping    # PONG
```

Compose already provides the healthcheck (`redis-cli ping`), `restart:
unless-stopped`, the `REDIS_PORT` host port (default 6379) and a named volume.
No Compose changes were needed in P1-T08. The volume is Redis' default
snapshotting; it is harmless for a cache and not relied upon.

## Environment variables

| Variable | Default | Meaning |
|---|---|---|
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host. Use `redis` if the API runs inside `shopsphere-network`. |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port (match `REDIS_PORT` if you changed it). |

No password is configured (local development only, like the rest of the
Compose stack). Connect and command timeouts are 2s.

## How caching works

```java
@Cacheable(cacheNames = "demo", key = "#key")   // hit: return from Redis; miss: run method, store result
@CachePut(cacheNames = "demo", key = "#key")    // always run method, overwrite the entry (refresh)
@CacheEvict(cacheNames = "demo", key = "#key")  // delete the entry
```

- Redis key: `<cacheName>::<key>`, e.g. `demo::abc`.
- Values are **JSON** with an `@class` type hint (not Java native
  serialization), so they round-trip to their original type, including records
  and `java.time` values. Only classes under `com.shopsphere.api` and basic JDK
  types can be deserialized. Cached values must be plain, JSON-friendly types.
- `null` results are not cached.
- Add new cache names to `CacheNames`.

## TTL and eviction

| Cache | TTL | Setting |
|---|---|---|
| any cache without an override | 10 minutes | `app.cache.default-ttl` |
| `demo` | 5 minutes | `app.cache.ttls.demo` |

Add a per-cache TTL with `app.cache.ttls.<cacheName>=<duration>` (e.g. `30s`,
`15m`). Redis deletes entries when the TTL elapses; the next call is a miss and
reloads. `@CacheEvict` removes an entry immediately and `@CachePut` replaces it
(and resets its TTL).

## When Redis is unavailable (degraded mode)

Strategy: **the app starts and keeps serving without Redis.**

- Lettuce connects lazily, so startup does not fail.
- A cache operation that fails is logged as a `WARN` (cache name and exception
  type only, no keys, values or connection details) and the underlying method
  runs normally. Callers never see Redis errors; there is no retry or circuit
  breaker.
- Cost: every call is uncached, and each failed attempt waits up to the 2s
  timeout.
- `/actuator/health` reports **DOWN (503)** while Redis is unreachable, so the
  outage is visible; `/api/v1/health` stays UP. If an orchestrator later uses
  `/actuator/health` for restarts, exclude Redis from that group
  (`management.health.redis.enabled=false`).
- Direct `StringRedisTemplate` use (as in the demo's `store/retrieve/remove`)
  is not covered by the cache error handler; callers of it must handle
  failures themselves.

## Verify

```bash
docker compose up -d redis
cd backend/shopsphere-api
set -a; . ../../.env; set +a; export JWT_SECRET=<32+ chars>
./mvnw test                      # includes the Redis tests
curl localhost:8080/actuator/health   # {"status":"UP"} with Redis up (app running)
```

Tests (`src/test/java/.../cache/`):
- `RedisCacheIntegrationTest` (needs the Compose Redis, not the database):
  connection, write/read/delete, direct TTL expiry, cache miss then hit,
  eviction, `@CachePut` refresh, cache TTL expiry (2s test TTL), JSON storage
  and type round trip, default TTL.
- `CacheDegradedModeTest` (needs no Redis): cache failures fall through to the
  real method.

Tests follow the project's existing approach of using the Compose services
(like `ShopsphereApiApplicationTests` for PostgreSQL); Testcontainers is not
used.
