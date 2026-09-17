# ADR-003: Architecturally Separate AI AMS from ShopSphere Core

## Status

Accepted

## Context

AI AMS is being built to observe, analyze, and eventually assist in
operating ShopSphere. There is a natural temptation to embed AI-driven
logic directly inside ShopSphere's business services (e.g., calling an AI
agent from within an order service, or having AI logic influence business
transactions directly). This project needs an explicit decision on whether
AI AMS is embedded within ShopSphere's core application or kept as a
architecturally distinct system.

## Decision

AI AMS is architecturally separated from the ShopSphere core application.
It is built, deployed, scaled, and operated as its own platform (Python/
FastAPI, its own data stores, its own deployment pipeline), integrating
with ShopSphere only through the event-driven and controlled-API mechanisms
defined in [ADR-002](ADR-002-event-driven-architecture.md) and
[`ai-ams-architecture.md`](../architecture/ai-ams-architecture.md).

AI AMS must never become part of ShopSphere's core business logic: it does
not decide business outcomes (pricing, order acceptance, inventory
allocation), and ShopSphere's business services do not call into AI AMS
synchronously as part of a business transaction.

## Reasons

- **AI should not be embedded in core business services.** Business logic
  must remain deterministic, auditable in the traditional sense, and owned
  by ShopSphere's domain teams. Embedding AI decision-making inside it
  would blur accountability for business outcomes and make ShopSphere's
  correctness depend on AI behavior.
- **Independent deployment.** AI AMS ships on its own release cadence,
  without requiring a ShopSphere deployment, and vice versa.
- **Independent scaling.** AI workloads (agent orchestration, RAG
  retrieval) have different, often bursty, resource profiles than
  ShopSphere's transactional workloads; independent scaling avoids one
  system's load affecting the other.
- **AI failure should not bring down ShopSphere.** If AI AMS is
  unavailable, slow, or misbehaving, ShopSphere's business operations
  must continue unaffected — this is only possible if AI AMS is not in
  ShopSphere's critical path.
- **Security isolation.** AI AMS is treated as a distinct, lower-trust
  principal (see [`system-architecture.md`](../architecture/system-architecture.md)
  section 1.6); keeping it a separate system makes this trust boundary
  enforceable rather than incidental.
- **Independent technology stack.** AI AMS's Python/FastAPI/LangGraph
  stack is suited to AI workloads and need not match ShopSphere's Java/
  Spring Boot stack.
- **Ability to evolve AI architecture independently.** Agent design,
  orchestration frameworks, and guardrail mechanisms are an active,
  fast-moving area; separation lets this evolve without touching
  ShopSphere's codebase.

## Consequences

**Benefits:**
- Clear accountability: ShopSphere owns business correctness, AI AMS owns
  operational intelligence.
- ShopSphere availability is not a function of AI AMS health.
- Each system can choose the technology and release cadence appropriate to
  its purpose.

**Trade-offs:**
- Cross-system integration (events, APIs) must be deliberately designed
  and versioned, rather than relying on in-process calls.
- Some operational latency is introduced (AI AMS's view of ShopSphere is
  not instantaneous) — see [ADR-002](ADR-002-event-driven-architecture.md)
  consequences.
- Duplication of some cross-cutting concerns (e.g., both systems need
  their own logging/config/security setup) since they do not share a
  runtime or codebase.
- Requires ongoing discipline (code review, CI checks) to prevent
  accidental coupling, as already flagged in
  [ADR-001](ADR-001-monorepo.md).

This decision does not prevent AI AMS from being deeply integrated
operationally — it constrains *how* that integration happens, not whether
it happens.
