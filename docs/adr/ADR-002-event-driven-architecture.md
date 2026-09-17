# ADR-002: Use Event-Driven Architecture for ShopSphere / AI AMS Communication

## Status

Accepted

## Context

ShopSphere and AI AMS need to communicate without tightly coupling
ShopSphere's business services to AI AMS. AI AMS needs visibility into
operational and domain activity (orders placed, inventory changes,
deployments, alerts) in order to observe, analyze, and eventually act — but
must not become a synchronous dependency of any ShopSphere business
transaction, and must not require ShopSphere business services to know
about AI AMS's internals.

A decision was needed on the primary mechanism for this cross-system
communication: synchronous request/response (direct API calls) versus
asynchronous messaging.

## Decision

Use RabbitMQ for asynchronous, event-driven communication as the default
mechanism for ShopSphere → AI AMS integration, and for other appropriate
cross-service communication within ShopSphere where a producer should not
block on, or be coupled to, its consumers.

Synchronous REST remains appropriate for direct request/response
interactions (frontend → backend; narrow, explicitly authorized read
queries) — see [`system-architecture.md`](../architecture/system-architecture.md)
section 1.4. Event-driven messaging is preferred specifically for
integration that crosses the ShopSphere/AI AMS boundary.

## Reasons

- **Loose coupling.** Producers (ShopSphere) publish events without
  knowing which consumers (AI AMS agents, future consumers) exist or how
  they process the event.
- **Asynchronous processing.** AI AMS analysis (incident detection, RCA)
  can take longer than a request/response cycle would tolerate; events let
  this happen off the critical path of any ShopSphere transaction.
- **Scalability.** Event consumers can be scaled independently of
  producers, and slow consumers do not throttle producers.
- **Resilience.** If AI AMS is down or degraded, ShopSphere continues
  operating; events queue and are processed when AI AMS recovers, rather
  than ShopSphere requests failing.
- **Independent evolution.** ShopSphere and AI AMS can change their
  internal implementations independently as long as the event contract is
  respected, supporting the separation established in
  [ADR-003](ADR-003-ai-ams-separation.md).
- **AI workload isolation.** AI AMS's often bursty, compute-heavy workloads
  (agent reasoning, RAG retrieval) are isolated from ShopSphere's
  transactional workloads instead of competing for the same request path.

## Consequences

**Benefits:**
- ShopSphere's availability and latency are not dependent on AI AMS.
- AI AMS can be deployed, scaled, and evolved independently.
- New AI AMS consumers can subscribe to existing events without
  ShopSphere changes.

**Costs:**
- **Eventual consistency.** AI AMS's view of ShopSphere state lags real
  time by the time it takes to publish, deliver, and process an event.
- **Message failures.** Broker or consumer failures can drop or delay
  events if not handled deliberately (dead-letter queues, retries).
- **Retries.** Failed processing requires a retry strategy, which must
  avoid unbounded reprocessing.
- **Duplicate messages.** At-least-once delivery semantics mean consumers
  must handle duplicate events (idempotent processing).
- **Ordering considerations.** Events may arrive out of order across
  queues/partitions; consumers that depend on ordering must handle this
  explicitly.
- **Operational complexity.** Running and monitoring a message broker adds
  infrastructure and operational overhead compared to direct calls.

This ADR does not claim RabbitMQ or asynchronous messaging is universally
superior to synchronous REST — each is used where it fits (see
[`system-architecture.md`](../architecture/system-architecture.md) section
1.4). No RabbitMQ configuration, exchanges, queues, or code are implemented
as part of this decision; that occurs in later implementation tickets.
