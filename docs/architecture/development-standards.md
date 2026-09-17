# Development Standards

**Status: engineering standard.** This document defines coding, API,
observability, and security standards for all future work across
ShopSphere and AI AMS. It is a contract for later implementation tickets,
not an implementation itself — **no dependencies, code, or configuration
are added by this ticket.**

## 1. Technology stack

### Backend (ShopSphere)
- Java 21
- Spring Boot
- Maven
- JUnit, Mockito
- Testcontainers

### Frontend (ShopSphere)
- React
- **JavaScript — not TypeScript.** This project explicitly uses
  React + JavaScript + Vite.
- Vite
- ESLint

### AI (AI AMS)
- Python
- FastAPI
- Pydantic
- pytest
- LangGraph
- LangChain

### Infrastructure
- Docker
- Kubernetes
- Terraform
- AWS

## 2. General coding standards

- **Clean code.** Code should read clearly without needing extensive
  comments to explain what it does.
- **SOLID, where appropriate.** Applied pragmatically — do not force
  patterns onto simple problems.
- **Meaningful naming.** Names describe intent and domain meaning, not
  implementation detail.
- **Small, focused classes/functions.** A unit does one thing; if
  describing it needs "and," it likely needs splitting.
- **Separation of concerns.** Presentation, orchestration, business logic,
  and persistence are not mixed in one place (see
  [`backend-architecture.md`](backend-architecture.md)).
- **Configuration externalization.** No environment-specific values
  hardcoded in source; configuration is injected via environment/config
  files.
- **No secrets in source code.** Credentials, keys, and tokens are never
  committed; they are supplied via secret management/environment at
  runtime.
- **Consistent error handling.** Errors follow one model per system (see
  section 4) rather than ad hoc handling per module.
- **Structured logging.** Logs are structured (not free-text) so they are
  queryable once aggregated (see section 3).
- **Automated testing.** New logic ships with unit tests at minimum;
  integration tests where cross-component behavior matters.
- **Code review.** All changes are reviewed before merge.
- **Backward compatibility.** Public API and event contract changes are
  additive/versioned rather than breaking existing consumers (see
  [ADR-004](../adr/ADR-004-api-versioning.md)).

## 3. Observability standards (FUTURE)

**Not implemented. These are target standards for later tickets.**

Every service (ShopSphere and AI AMS) is expected to produce three
observability pillars:

```
Application
  |
  +-- Logs    ------> ELK / OpenSearch
  |
  +-- Metrics ------> Prometheus
  |
  +-- Traces  ------> OpenTelemetry
```

- **Logs** — structured, correlation-ID-tagged application and event logs.
- **Metrics** — service-level (latency, error rate, throughput) and
  business-relevant metrics.
- **Traces** — distributed traces correlating a request/event across
  service boundaries.

These signals are consumed by human operators (dashboards, alerts) and,
critically, become evidence that AI AMS's Incident Detection and RCA
agents reason over (see
[`ai-ams-architecture.md`](ai-ams-architecture.md)). Observability
tooling and instrumentation are not implemented as part of this ticket.

## 4. Security standards (FUTURE)

**Not implemented. These are target principles for later tickets. This
ticket does not implement Spring Security, JWT, or any auth mechanism.**

- **Authentication** — verifying identity of a caller (user, service, or
  agent) before granting access.
- **Authorization** — verifying a caller may perform the specific action
  requested.
- **RBAC** — access is governed by role, not by ad hoc per-user checks.
- **Least privilege** — every principal (human or agent) is granted the
  minimum access needed for its purpose.
- **Secret management** — credentials/keys are issued and rotated through
  a secret manager, never embedded in code or config committed to source
  control.
- **Service-to-service authentication** — internal calls between
  ShopSphere services, and between ShopSphere and AI AMS, are
  authenticated, not implicitly trusted by network location alone.
- **Audit logging** — security-relevant actions (auth events, privileged
  operations, AI-initiated actions) are logged immutably.
- **Secure API boundaries** — all externally reachable APIs validate input,
  enforce authN/authZ, and do not leak internal implementation details in
  errors.
- **AI action authorization** — any operational action an AI agent
  proposes is authorized through the guardrail/approval flow in
  [`ai-ams-architecture.md`](ai-ams-architecture.md) section 4, never
  through ambient/implicit trust.

### Conceptual roles

| Role | Description |
|------|-------------|
| `CUSTOMER` | End user of the ShopSphere storefront; access limited to their own data and public catalog/ordering actions. |
| `SUPPORT_AGENT` | Human support staff; can view/assist with customer issues within defined limits. |
| `OPERATIONS` | Human operator responsible for running ShopSphere; broader operational visibility and action rights than support. |
| `ADMIN` | Full administrative access within ShopSphere. |
| `AMS_AGENT` | Identity used by AI AMS agents when interacting with ShopSphere. **Restricted privileges**: read/observe-oriented by default, with any write/operational action gated by the guardrail and human-approval flow — never granted `ADMIN`-equivalent access. |

These roles are conceptual; no authentication/authorization mechanism is
implemented by this ticket.

## 5. API design principles

Standards for all ShopSphere (and, where applicable, AI AMS) HTTP APIs.
No endpoints are implemented by this ticket.

### Conventions

```
GET    /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/orders
```

- **REST conventions** — resources are nouns; HTTP verbs express the
  action (`GET` read, `POST` create, `PUT`/`PATCH` update, `DELETE`
  remove).
- **HTTP status conventions** — `2xx` success, `4xx` client error (e.g.,
  `400` validation, `401`/`403` auth, `404` not found, `409` conflict),
  `5xx` server error.
- **Validation** — request payloads are validated at the API boundary;
  invalid requests fail fast with a `4xx` and a clear error body.
- **Consistent error responses** — one error shape across all APIs (e.g.,
  error code, human-readable message, correlation ID), per
  [`backend-architecture.md`](backend-architecture.md) rule 11.
- **Pagination** — list endpoints are paginated by default, not expected
  to return unbounded result sets.
- **Filtering** and **sorting** — list endpoints support standard query
  parameters for filtering and sorting rather than bespoke per-endpoint
  variants.
- **Correlation/trace IDs** — every request carries or is assigned a
  correlation ID, propagated through logs and downstream calls/events.
- **Idempotency** — operations that may be retried (e.g., payment
  submission) support idempotency keys so retries don't duplicate effects.

See [ADR-004](../adr/ADR-004-api-versioning.md) for the versioning
approach these conventions sit within.

## 6. Architectural principles

Project-wide principles that apply across ShopSphere and AI AMS:

1. **Separation of concerns** — distinct responsibilities live in distinct
   layers/modules.
2. **Domain-oriented design** — code is organized around business domains,
   not technical layers alone.
3. **Loose coupling** — systems and modules depend on contracts
   (APIs/events), not on each other's internals.
4. **API-first thinking** — interfaces are designed deliberately before
   implementation details.
5. **Event-driven integration where appropriate** — asynchronous events
   are preferred for cross-system integration (see
   [ADR-002](../adr/ADR-002-event-driven-architecture.md)).
6. **Security by design** — access control and data protection are
   considered from the start, not retrofitted.
7. **Observability by design** — logs, metrics, and traces are planned as
   part of a component's design, not added as an afterthought.
8. **Testability** — code is structured so it can be tested without
   excessive setup or mocking gymnastics.
9. **Automation** — repetitive manual work (build, test, deploy) is
   automated rather than performed by hand.
10. **AI safety and controlled autonomy** — AI AMS actions are constrained
    by guardrails and, where warranted, human approval; autonomy is
    granted incrementally and deliberately, never assumed.
11. **Infrastructure as code** — infrastructure is defined declaratively
    (Terraform, Kubernetes manifests) and version-controlled, not
    hand-configured.
12. **Configuration externalization** — environment-specific configuration
    lives outside source code.
