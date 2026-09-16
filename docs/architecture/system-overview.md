# System Overview

**Status: planning document. This describes intended, future architecture.
No components described below are implemented as of P1-T01.**

## Purpose

This document gives a high-level view of how ShopSphere (the e-commerce
application) and AI AMS (the AI management platform) are intended to relate
to one another. It is intentionally high-level — detailed component design
is out of scope for this ticket and will be produced in later architecture
tickets.

## High-Level Diagram (Planned)

```
                        ShopSphere
                            |
                            v
                 Spring Boot Backend  [planned]
                            |
            +---------------+---------------+
            |               |               |
            v               v               v
       REST APIs      Domain Events   Observability
       [planned]        [planned]       [planned]
            |
            v
                    AI AMS Platform  [planned]
                            |
        +-------------------+--------------------+
        |          |            |         |       |
        v          v            v         v       v
    FastAPI   Multi-Agent      RAG   Guardrails  Human
   [planned]    System      [planned] [planned] Approval
              [planned]                        [planned]
                                                    |
                                                    v
                                          Automated Remediation
                                                [planned]
```

## Component Descriptions (All Planned / Future Phases)

### ShopSphere Backend — Spring Boot (Planned, Phase P2)
Will expose REST APIs for the e-commerce domain (products, orders,
customers, inventory) and emit domain events that the AI AMS platform can
consume. Observability (metrics, tracing, logging) will be added as part of
backend implementation, not this ticket.

### ShopSphere Frontend — React (Planned, Phase P3)
Will be a React/Vite single-page application consuming the ShopSphere
backend's REST APIs. Not implemented yet.

### AI AMS — FastAPI Service (Planned, Phase P4)
Will be the entry point for the AI platform, exposing endpoints that
downstream agent workflows use. Not implemented yet.

### AI AMS — Multi-Agent System (Planned, Phase P4)
Will coordinate multiple specialized agents (e.g., diagnosis, planning,
remediation) using LangGraph/LangChain. Not implemented yet.

### AI AMS — RAG (Planned, Phase P5)
Will provide retrieval-augmented generation over ShopSphere documentation,
logs, and telemetry to ground agent responses. Not implemented yet.

### AI AMS — Guardrails (Planned, Phase P5)
Will constrain agent behavior and outputs to safe, approved actions. Not
implemented yet.

### AI AMS — Human Approval (Planned, Phase P5)
Will introduce human-in-the-loop checkpoints before higher-risk automated
actions are executed. Not implemented yet.

### AI AMS — Automated Remediation (Planned, Phase P5)
Will allow approved agent workflows to take corrective action against
ShopSphere (e.g., restarting a service, rolling back a change) under
guardrail and approval constraints. Not implemented yet.

## Boundaries

ShopSphere and AI AMS are designed as two independently deployable systems
that communicate over well-defined interfaces (REST APIs, domain events).
They are co-located in one repository for coordinated development, not
tightly coupled at the code level. See
[`docs/adr/ADR-001-monorepo.md`](../adr/ADR-001-monorepo.md) for the
reasoning behind the monorepo structure and how boundaries are maintained.

## Out of Scope for This Document

- Detailed API contracts
- Data models / database schema
- Deployment topology
- Security architecture details
- Agent orchestration design

These will be addressed in dedicated architecture and design tickets in
later phases.
