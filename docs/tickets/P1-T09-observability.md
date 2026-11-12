# P1-T09 - Observability

**Commit:** `feat: establish observability with prometheus and grafana`

## Delivered

- Actuator exposes `health`, `info`, `metrics`, `prometheus` (nothing else).
  `health`/`info` public, per-component status only (db, redis), no details;
  `metrics`/`prometheus` behind HTTP Basic (`ActuatorSecurityConfig`), locked
  for everyone unless `ACTUATOR_PASSWORD` is set.
- Metrics via Micrometer + `micrometer-registry-prometheus`: JVM, HTTP (with
  latency histogram), HikariCP, Lettuce (Redis), cache hit/miss. No custom
  metrics. Tag `application=shopsphere-api`.
- Logging: console text with request id; rotated JSON (ECS) file; access log
  per request; startup/shutdown markers; auth events without PII. New
  `RequestLoggingFilter` (adds/echoes `X-Request-Id`), `LifecycleLogger`.
- Build info in `/actuator/info` (spring-boot-maven-plugin `build-info`).
- Docker: `backend/shopsphere-api/Dockerfile`; Compose adds `prometheus`
  (v3.0.1, 15d retention, volume), `grafana` (11.4.0, volume) and an optional
  `api` service under `--profile app`. Existing services untouched.
- Grafana: provisioned Prometheus data source and "ShopSphere Overview"
  dashboard (health, request rate, error rate, p50/p95/p99, JVM heap/memory,
  threads, DB pool).
- Docs: `docs/architecture/observability.md` (URLs, env vars, verification).

## Verify

See `docs/architecture/observability.md`. Short version:

```bash
export JWT_SECRET=<32+ chars> ACTUATOR_PASSWORD=<password>
docker compose --profile app up -d --build
curl localhost:8080/actuator/health
# Prometheus http://localhost:9090/targets   Grafana http://localhost:3000
```

Result when delivered: 41 tests passed (7 new: health components, info,
metrics auth, unexposed endpoints, metric families, request ids, locked by
default). In Compose all services healthy; Prometheus target UP; metrics
queried in Prometheus and through Grafana's data source; logs showed request
ids and JSON file output.

## Notes

- The API runs in Compose only with `--profile app`; plain
  `docker compose up -d` still starts infrastructure (plus Prometheus and
  Grafana, where the API target is DOWN until the API runs).
- `/actuator/health` is DOWN (503) when Postgres or Redis is unreachable.
- Dashboard panels were verified by querying through Grafana's data source,
  not by viewing them in a browser.
- Prometheus has no auth and Grafana uses the local default password
  (`change-me`); local development only.

## Out of scope

ELK/OpenTelemetry/tracing, alerting rules, AI incident detection, LLM
monitoring, Kubernetes/Terraform.
