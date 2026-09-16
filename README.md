# ShopSphere + AI AMS

## 1. Project Overview

This repository is the enterprise monorepo for two related but independently
bounded systems:

- **ShopSphere** — a production-style e-commerce application (backend + frontend).
- **AI AMS (Application Management System)** — an AI-driven platform that will
  observe, analyze, and (in later phases) assist in managing ShopSphere.

This ticket (P1-T01) establishes only the **repository skeleton**: directory
structure, root documentation, and baseline tooling configuration. **No
application code, services, or infrastructure have been implemented yet.**

## 2. ShopSphere

ShopSphere is the planned e-commerce application, split into a Java/Spring
Boot backend and a React/JavaScript frontend. Planned domains include
product catalog, ordering, customer management, and inventory — none of
which are implemented in this ticket.

## 3. AI AMS

AI AMS is the planned AI platform that will sit alongside ShopSphere. It is
intended to provide multi-agent analysis, retrieval-augmented generation
(RAG), guardrails, human-in-the-loop approval, and automated remediation
capabilities. None of these capabilities exist yet — this ticket only
reserves the directory structure for the future `ai/` codebase.

## 4. High-Level Architecture (Planned)

```
ShopSphere Frontend (React)
        |
        v
Spring Boot Backend --- REST APIs
        |               Domain Events
        |               Observability
        v
   AI AMS Platform (FastAPI, Multi-Agent, RAG, Guardrails,
                     Human Approval, Automated Remediation)
```

See [`docs/architecture/system-overview.md`](docs/architecture/system-overview.md)
for more detail and explicit implemented-vs-planned labeling.

## 5. Repository Structure

```
shopsphere/                    (this repository)
├── backend/
│   ├── shopsphere-api/        # Spring Boot API service (planned)
│   ├── shopsphere-common/     # Shared backend libraries (planned)
│   └── shopsphere-security/   # Auth/security module (planned)
│
├── frontend/
│   └── shopsphere-web/        # React/Vite web app (planned)
│
├── ai/
│   ├── ams-api/                # FastAPI service (planned)
│   ├── ams-agents/             # Multi-agent system (planned)
│   └── ams-common/             # Shared AI/AMS libraries (planned)
│
├── infrastructure/
│   ├── docker/                 # Container definitions (planned)
│   ├── kubernetes/             # K8s manifests (planned)
│   └── terraform/              # IaC for AWS (planned)
│
├── n8n/
│   └── workflows/               # Workflow automation (planned)
│
├── docs/
│   ├── architecture/            # Architecture documentation
│   ├── adr/                     # Architecture Decision Records
│   └── api/                     # API documentation (planned)
│
├── scripts/                     # Developer/ops scripts (planned)
│
├── .gitignore
├── .editorconfig
└── README.md
```

## 6. Planned Technology Stack

**Note: technologies below are planned targets only. Nothing listed here has
been installed or configured as part of this ticket.**

**Backend**
- Java 21
- Spring Boot
- Maven

**Frontend**
- React
- JavaScript
- Vite

**AI**
- Python
- FastAPI
- LangGraph
- LangChain

**Infrastructure**
- Docker
- Kubernetes
- Terraform
- AWS

## 7. Development Principles

- **Strong module boundaries.** ShopSphere (business application) and AI AMS
  (AI platform) are developed as clearly separated codebases within a shared
  repository, not intermingled.
- **Incremental, ticket-scoped delivery.** Each phase/ticket implements a
  well-defined, reviewable slice of functionality.
- **No speculative implementation.** Code is not written ahead of the ticket
  that requires it — this avoids drift between documented and actual state.
- **Documentation-first for architecture decisions.** Significant decisions
  are captured as ADRs (see `docs/adr/`).
- **Secrets never committed.** Environment-specific secrets are excluded via
  `.gitignore`; only `.env.example` templates are tracked.

## 8. Planned Implementation Phases

| Phase | Scope (high level) |
|-------|---------------------|
| P1 | Repository skeleton, project scaffolding, foundational docs (this ticket: P1-T01) |
| P2 | ShopSphere backend core services (API, security, domain services) |
| P3 | ShopSphere frontend application |
| P4 | AI AMS platform core (FastAPI service, agent framework) |
| P5 | RAG, guardrails, human approval, automated remediation |
| P6 | Infrastructure & deployment (Docker, Kubernetes, Terraform, AWS) |
| P7 | Workflow automation (n8n) and observability |

Phase boundaries and scope are subject to refinement in later planning
tickets. **As of this ticket, only the repository skeleton and documentation
described above exist.**
