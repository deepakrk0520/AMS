# AI AMS Architecture

**Status: architectural contract.** This document defines AI AMS as a
separate operational intelligence platform and establishes the standards
future AI AMS implementation tickets must follow. **No agents, RAG
pipeline, guardrails, or FastAPI endpoints are implemented by this
ticket** — everything below describes target architecture unless noted
otherwise.

## Guiding principle

> **ShopSphere runs the business. AI AMS operates around ShopSphere** by
> observing, analyzing, and assisting — and, only where explicitly
> authorized, executing operational actions.

AI AMS must never become part of ShopSphere's core business logic. It does
not process orders, manage inventory, or make business decisions. It
manages the operational health of the systems that do. See
[ADR-003](../adr/ADR-003-ai-ams-separation.md) for the separation decision.

## Conceptual structure

```
AI AMS
 |
 +-- FastAPI API
 |
 +-- Agent Orchestrator
 |
 +-- Incident Detection Agent
 |
 +-- RCA Agent
 |
 +-- Knowledge/RAG Agent
 |
 +-- Remediation Agent
 |
 +-- Guardrails
 |
 +-- Human Approval
 |
 +-- Audit
 |
 +-- External Systems
```

### Component responsibilities

- **FastAPI API** — the platform's external interface: receives
  operational events/requests, exposes status and results, and is the only
  entry point into AI AMS from the outside.
- **Agent Orchestrator** — coordinates the lifecycle of a diagnostic or
  operational workflow across the specialized agents below (e.g., using
  LangGraph); owns workflow state, not business logic.
- **Incident Detection Agent** — identifies operational anomalies/incidents
  from observability and event signals.
- **RCA Agent** — investigates a detected incident and produces structured
  root-cause reasoning.
- **Knowledge/RAG Agent** — retrieves relevant operational knowledge to
  ground other agents' reasoning.
- **Remediation Agent** — proposes, and where authorized, executes
  operational actions.
- **Guardrails** — validates any agent-proposed action against risk policy
  before it can proceed toward execution.
- **Human Approval** — gate for actions guardrails classify as requiring a
  human decision.
- **Audit** — immutable record of what was observed, recommended,
  approved/rejected, and executed, for accountability and rollback.
- **External Systems** — ShopSphere (via events/APIs), observability
  stacks, n8n, and any ticketing/notification systems AI AMS integrates
  with.

## Multi-agent architecture

### Incident Detection Agent

Responsible for identifying operational anomalies and incidents.

Potential inputs:
- metrics
- logs
- traces
- application events
- alerts

Output: a detected incident, handed to the RCA Agent for investigation.

### RCA Agent

Responsible for investigating incidents.

Potential evidence:
- logs
- metrics
- traces
- deployments
- application events
- runbooks
- historical incidents
- knowledge base

The RCA agent should produce structured reasoning/output such as:

```
incident:                <what was detected>
evidence:                <signals examined>
possible causes:         <ranked hypotheses>
confidence:              <per-hypothesis confidence>
recommended next steps:  <diagnostic or remediation suggestions>
```

This structure — not a specific schema/format — is the standard; exact
representation is defined in the implementation ticket.

### Knowledge/RAG Agent

Responsible for retrieving relevant operational knowledge to support RCA
and remediation decisions.

Potential knowledge sources:
- runbooks
- SOPs
- architecture documentation
- historical incidents
- troubleshooting guides

### Remediation Agent

Responsible for proposing, and — subject to guardrails and authorization —
executing operational actions.

Examples (illustrative only, not an exhaustive or committed list):
- restart service
- scale service
- clear cache
- rollback deployment

No remediation action is executed by this agent without passing through
the guardrail and approval flow defined below.

## AI safety / guardrail architecture

**Principle: AI must not directly execute unrestricted operational
actions.**

Preferred flow:

```
AI recommendation
      |
      v
Risk classification
      |
      v
Guardrail validation
      |
      +----------------------+
      |                      |
   Low risk               High risk
      |                      |
      v                      v
Automated action       Human approval
      |                      |
      +----------+-----------+
                  |
                  v
              Execution
```

Standards this flow must eventually satisfy:

- **Least privilege** — each agent and the remediation execution path holds
  only the permissions required for its narrow purpose, never broad
  ShopSphere operator/admin access.
- **Action allowlists** — only explicitly allowlisted action types may be
  executed automatically; anything outside the allowlist requires human
  approval regardless of risk classification.
- **Validation** — every proposed action is validated against current
  system state before execution (e.g., don't roll back a deployment that
  already rolled back).
- **Human approval** — required for any action classified as high risk, or
  outside the automated allowlist.
- **Audit trail** — every recommendation, classification, approval/
  rejection, and execution outcome is recorded immutably.
- **Rollback capability** — actions that can be automated should have a
  known, tested rollback/undo path.
- **Action timeout** — actions awaiting approval or execution have a bounded
  time window; stale actions are not executed against outdated state.
- **Failure handling** — a failed action is surfaced (not silently
  retried indefinitely) and recorded in the audit trail.

None of these mechanisms are implemented by this ticket; they are
standards for the AI safety implementation tickets.

## AI AMS / ShopSphere communication

AI AMS does not integrate by direct invocation. This is explicitly
**not** the model:

```
OrderService -> directly invokes AI Agent      (NOT preferred)
```

The preferred model is event-driven:

```
ShopSphere
    |
    v
Domain/Operational Event
    |
    v
RabbitMQ
    |
    v
AI AMS
```

Controlled, versioned REST API access is used only where an event/telemetry
model is insufficient (e.g., an agent needs to look up current state for an
investigation), and always through explicitly approved, narrow-scope
endpoints — never ad hoc calls into internal ShopSphere services.

**Why loose coupling matters:** ShopSphere must be able to operate,
release, and evolve independently of AI AMS. If AI AMS is unavailable,
degraded, or being redeployed, ShopSphere's business operations must be
unaffected. Direct/synchronous coupling would make ShopSphere's request
path dependent on AI AMS availability and would tie the two systems'
release cadences together — both of which contradict the separation
principle in [ADR-003](../adr/ADR-003-ai-ams-separation.md). Event-driven
integration keeps AI AMS a consumer/observer of ShopSphere activity rather
than a dependency of it (see also
[ADR-002](../adr/ADR-002-event-driven-architecture.md)).
