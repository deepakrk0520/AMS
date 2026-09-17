# System Architecture

**Status: architectural contract.** This document defines how ShopSphere and
AI AMS are architected to work together. It describes target architecture;
sections describing components or infrastructure not yet built are
explicitly marked **(Future)**. See
[`docs/architecture/system-overview.md`](system-overview.md) for the
original P1-T01 planning summary and
[`docs/adr/ADR-001-monorepo.md`](../adr/ADR-001-monorepo.md),
[`ADR-003-ai-ams-separation.md`](../adr/ADR-003-ai-ams-separation.md) for the
decisions behind the separation described here.

## 1.1 System overview

**ShopSphere** is the business application. It runs the e-commerce domain:
product catalog, ordering, inventory, customers, and payments. It is the
system of record for business data and the system users and store operators
interact with directly.

**AI AMS** (AI Application Management System) is an operational intelligence
platform that runs alongside ShopSphere. It observes ShopSphere's behavior
(via APIs, events, and observability signals), analyzes operational health,
assists operators with diagnosis and knowledge retrieval, and — only where
explicitly authorized — executes operational remediation actions.

The foundational rule governing this project:

> **ShopSphere runs the business. AI AMS operates around ShopSphere.**
> AI AMS must never become part of core ShopSphere business logic.

## 1.2 Logical architecture

```
Frontend (React)
      |
      v
ShopSphere Backend (Spring Boot)
      |
      v
Business Services (product, order, inventory, customer, payment)
      |
      v
Data / Messaging / Observability (PostgreSQL, Redis, RabbitMQ, logs/metrics/traces)
      |
      v
AI AMS (FastAPI + multi-agent platform)
```

Data and control flow one direction at the top (frontend → backend →
services → data), and operational signals flow onward into AI AMS at the
bottom. AI AMS does not sit inline in the request path of any ShopSphere
user-facing transaction.

## 1.3 Major components

These are architectural responsibilities, not implementations.

- **React frontend** — presents the ShopSphere storefront/admin experience
  to end users; consumes ShopSphere REST APIs only. Plain JavaScript, not
  TypeScript (see [`development-standards.md`](development-standards.md)).
- **Spring Boot backend** — owns ShopSphere business logic and domain
  rules; exposes REST APIs; emits domain events; is the sole writer of
  business data.
- **PostgreSQL** — system-of-record relational storage for ShopSphere
  business data (products, orders, customers, inventory, payments).
- **Redis** — caching and ephemeral state (e.g., sessions, hot reads) to
  reduce load on PostgreSQL and improve latency.
- **RabbitMQ** — asynchronous message broker carrying domain/operational
  events between ShopSphere and AI AMS, and between internal services.
- **Python/FastAPI AI AMS** — the AI platform's API surface and service
  runtime; independent deployable from ShopSphere.
- **Agent orchestration** — coordinates specialized agents (incident
  detection, RCA, knowledge/RAG, remediation) toward a diagnostic or
  operational goal. **(Future)**
- **RAG** — retrieves relevant operational knowledge (runbooks, past
  incidents, docs) to ground agent reasoning. **(Future)**
- **Guardrails** — validates AI-proposed actions against risk policy before
  they may execute. **(Future)**
- **Human approval** — gates higher-risk AI-proposed actions behind explicit
  human sign-off. **(Future)**
- **n8n** — workflow automation used for operational glue (notifications,
  ticketing, approval routing) around AI AMS. **(Future)**
- **Observability** — logs, metrics, and traces produced by ShopSphere and
  AI AMS; collected centrally and consumed both by humans and by AI AMS as
  evidence. **(Future)**
- **CI/CD** — Jenkins-based build/test/deploy pipelines for both systems,
  producing container images deployed independently. **(Future)**

## 1.4 Communication patterns

- **Synchronous REST** — used for direct, request/response interactions:
  frontend → ShopSphere backend, and any controlled/authorized queries AI
  AMS makes against ShopSphere APIs (e.g., read-only lookups needed for
  investigation).
- **Asynchronous event-driven** — used for cross-system, non-blocking
  communication: ShopSphere emits domain/operational events to RabbitMQ; AI
  AMS consumes them independently. This is the default and preferred
  pattern for ShopSphere → AI AMS integration (see
  [ADR-002](../adr/ADR-002-event-driven-architecture.md)).
- **AI AMS integration** — AI AMS never calls into ShopSphere business
  logic directly and ShopSphere never calls into AI AMS synchronously as
  part of a user-facing request. Integration is event-based, plus narrow,
  explicitly-authorized API access for read queries or approved remediation
  actions.

## 1.5 Data boundaries

Business data belongs to ShopSphere. PostgreSQL is owned exclusively by the
ShopSphere backend; no other system, including AI AMS, accesses it
directly. AI AMS gains visibility into ShopSphere state only through:

- **Domain/operational events** published to RabbitMQ,
- **Observability telemetry** (logs, metrics, traces),
- **Approved, versioned REST APIs** exposed intentionally for AI AMS
  consumption, where an event/telemetry model is insufficient.

AI AMS maintains its own data stores for its own concerns (agent memory,
knowledge base, audit trail); it does not persist or become a second
system of record for ShopSphere business entities.

## 1.6 Security boundary

Four trust zones are distinguished:

1. **User-facing application** — the React frontend and public ShopSphere
   APIs; treated as the least-trusted boundary, subject to standard
   authentication/authorization.
2. **Internal services** — ShopSphere backend modules and data stores;
   reachable only from within the trusted network, not directly from the
   internet.
3. **AI agents** — AI AMS components; treated as a distinct, lower-trust
   principal than internal ShopSphere services. Agents authenticate as
   themselves (e.g., an `AMS_AGENT` role, see
   [`development-standards.md`](development-standards.md)), never impersonate
   a human user, and are scoped to the minimum access needed to observe and
   recommend.
4. **Privileged operational actions** — any action that changes ShopSphere
   operational state (restart, scale, rollback, cache clear). These require
   guardrail validation and, for higher-risk actions, human approval,
   regardless of which agent proposes them. See
   [`ai-ams-architecture.md`](ai-ams-architecture.md) section 4.

## 1.7 Deployment view (FUTURE)

**Not implemented. This describes the intended future deployment path.**

```
Developer
    |
    v
Git
    |
    v
Jenkins (build, test, package)
    |
    v
Container Images
    |
    v
Kubernetes
    |
    v
AWS
```

ShopSphere and AI AMS are built, containerized, and deployed as
independent pipelines/artifacts, even though they share a Jenkins/K8s/AWS
platform. Neither system's deployment blocks the other's.

## 1.8 Observability (FUTURE)

**Not implemented. This describes the intended future observability model.**

Three pillars are produced by every service (ShopSphere and AI AMS alike):

- **Logs** — structured application/event logs, aggregated in
  ELK/OpenSearch.
- **Metrics** — service and business metrics, collected by Prometheus and
  visualized in Grafana.
- **Traces** — distributed request traces via OpenTelemetry, correlating
  activity across service boundaries.

These signals serve two audiences: human operators (dashboards, alerting)
and, eventually, AI AMS itself — logs, metrics, and traces become evidence
that the Incident Detection and RCA agents reason over (see
[`ai-ams-architecture.md`](ai-ams-architecture.md)). Observability
infrastructure is not implemented as part of this ticket.
