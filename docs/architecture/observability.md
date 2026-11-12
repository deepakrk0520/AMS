# Observability (P1-T09)

Logs, metrics and health for `shopsphere-api`, with Prometheus and Grafana
running in Docker Compose.

```
shopsphere-api ──/actuator/prometheus (Basic auth)──► Prometheus :9090 ──► Grafana :3000
      │ └─ /actuator/health, /info (public)
      ├─ console (text) + logs/shopsphere-api.log (JSON, rotated)
      └─ PostgreSQL, Redis (health + pool/client metrics)
```

## URLs

| What | URL | Notes |
|---|---|---|
| API | http://localhost:8080 | |
| Health | http://localhost:8080/actuator/health | public |
| Info | http://localhost:8080/actuator/info | public |
| Metrics | http://localhost:8080/actuator/metrics | Basic auth |
| Prometheus format | http://localhost:8080/actuator/prometheus | Basic auth |
| Prometheus UI | http://localhost:9090 | local only, no auth |
| Grafana | http://localhost:3000 | `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` |

Ports are overridable with `SERVER_PORT`, `PROMETHEUS_PORT`, `GRAFANA_PORT`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `ACTUATOR_PASSWORD` | none | Scrape password for `/actuator/metrics` and `/actuator/prometheus`. Without it they return 401 to everyone. Set the same value for the API and Prometheus (Compose passes it to both). |
| `ACTUATOR_USERNAME` | `prometheus` | Scrape user. `prometheus.yml` assumes the default. |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | `admin` / `change-me` | Grafana login. Local default only; change for anything shared. |
| `PROMETHEUS_PORT`, `GRAFANA_PORT` | 9090, 3000 | Host ports. |
| `LOG_DIR` | `logs` | Directory of the JSON log file (`/logs` in the container). |

`JWT_SECRET` (existing) is also required to start the API.

## Start and verify

```bash
export JWT_SECRET=<32+ chars> ACTUATOR_PASSWORD=<password>   # or put them in .env
docker compose --profile app up -d --build     # infra + Prometheus + Grafana + API
docker compose --profile app ps                # all services healthy

curl localhost:8080/actuator/health            # {"status":"UP","components":{"db":...,"redis":...}}
curl -u prometheus:$ACTUATOR_PASSWORD localhost:8080/actuator/prometheus | head
```

Then open http://localhost:9090/targets (job `shopsphere-api` should be UP) and
Grafana → Dashboards → ShopSphere → **ShopSphere Overview**. Generate traffic
(e.g. `for i in $(seq 20); do curl -s localhost:8080/api/v1/health; done`) and
the HTTP panels fill within one scrape interval (15s).

`docker compose up -d` without `--profile app` starts only infrastructure,
Prometheus and Grafana (the API target then shows DOWN). To run the API on the
host instead, point Prometheus at it by changing the target in
`infrastructure/prometheus/prometheus.yml` to `host.docker.internal:8080`.

## Actuator and security

Exposed endpoints: `health`, `info`, `metrics`, `prometheus` only. `env`,
`beans`, `configprops`, `heapdump`, `loggers` etc. are not exposed.

- `health` and `info` are public. Health lists each component's status
  (`db`, `redis`, ...) but never details, hosts or credentials. It reports
  DOWN (503) while PostgreSQL or Redis is unreachable.
- `metrics` and `prometheus` require HTTP Basic (`ActuatorSecurityConfig`),
  a separate in-memory user, not an application account. JWTs do not work there.
- `info` shows app name and build version/time only.
- Hardening later: serve actuator on a separate management port not published
  to the host, add TLS, and use secret storage for `ACTUATOR_PASSWORD`.

## Metrics

All built-in (Spring Boot + Micrometer), tagged `application="shopsphere-api"`.

| Area | Metrics |
|---|---|
| JVM | `jvm_memory_used_bytes` (heap / non-heap), `jvm_gc_*`, `jvm_threads_*` |
| HTTP | `http_server_requests_seconds_*` (count, duration histogram; tags status, method, uri, outcome) |
| DB pool | `hikaricp_connections_active / idle / pending / max`, `hikaricp_connections_acquire_seconds_*` |
| Redis | `lettuce_command_*` (Redis client command latency and counts); cache `cache_gets_total` (hit/miss), `cache_puts_total`, `cache_evictions_total`; Redis connectivity via `/actuator/health` (`redis` component) |

HTTP latency buckets include 50ms ... 2s so p95/p99 work in Grafana. No custom
metrics were added.

## Dashboard

`infrastructure/grafana/dashboards/shopsphere-overview.json`, provisioned
automatically with the Prometheus data source (`infrastructure/grafana/provisioning/`).
Panels: application health (scrape up/down), request rate, 5xx error rate, p95
latency, request rate by status, response time p50/p95/p99, 4xx/5xx rate, JVM
heap, heap vs non-heap, threads, DB pool (active/idle/pending/max), DB connection
acquire time. The dashboard is read-only from files; edit the JSON and restart
Grafana (or re-provision) to change it.

## Logging

Spring Boot's Logback defaults, configured in `application.properties`
(no custom Logback XML).

- **Console:** text, `LEVEL [requestId] logger : message`.
- **File:** JSON (ECS format) at `${LOG_DIR:logs}/shopsphere-api.log`, rotated at
  10MB, kept 14 days, capped at 500MB total. JSON with fields like `@timestamp`,
  `log.level`, `message`, `service.name`, `error.*` and the `requestId` MDC
  entry is easy to ingest later. In Compose, logs are in the `shopsphere-api-logs`
  volume (`docker compose logs api` shows the console output).
- **Request id:** every request gets `X-Request-Id` (a valid incoming one is
  kept, otherwise a UUID), echoed in the response and added to every log line.
- **Access log** (logger `http.access`): one line per request: method, path,
  status, duration. INFO for 2xx/3xx, WARN for 4xx, ERROR for 5xx; actuator
  scrapes are DEBUG. No query strings, headers, bodies or tokens.
- **Events:** startup/shutdown markers (`ShopSphere API started` /
  `shutting down`), registration and login success/failure (user id only;
  emails and passwords are never logged), cache failures (WARN), unexpected
  exceptions (ERROR with stack trace, generic message to the client).
- Levels: root INFO, `com.shopsphere` INFO. Change at runtime via env, e.g.
  `LOGGING_LEVEL_COM_SHOPSPHERE=DEBUG`.
