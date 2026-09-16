# ADR-001: Use a Monorepository for ShopSphere and AI AMS

## Status

Accepted

## Context

This project consists of two related but distinct systems:

1. **ShopSphere** — a Java/Spring Boot + React e-commerce application.
2. **AI AMS** — a Python-based AI platform (FastAPI, multi-agent workflows,
   RAG, guardrails, remediation) that observes and, in later phases, acts
   upon ShopSphere.

AI AMS exists specifically to support and manage ShopSphere, and the two
systems are expected to evolve together, especially in early phases where
their integration points (domain events, APIs, observability data) are
still being defined. A decision was needed on whether to host these systems
in separate repositories (a polyrepo approach) or within a single repository
(a monorepo approach).

## Decision

We will use a single monorepository (`shopsphere/`) containing both the
ShopSphere application and the AI AMS platform as clearly separated
top-level directories (`backend/`, `frontend/`, `ai/`), alongside shared
`infrastructure/`, `n8n/`, `docs/`, and `scripts/` directories.

Strong boundaries between ShopSphere and AI AMS will be maintained at the
code and dependency level (separate build systems, separate deployable
artifacts, communication only via REST APIs and domain events) even though
they share a repository.

## Reasons

- **Coordinated evolution.** ShopSphere and AI AMS are being designed and
  built together in the same project timeline; a monorepo makes it easy to
  evolve their integration points (event schemas, API contracts) in lockstep
  and review cross-system changes in a single pull request.
- **Single source of truth for architecture.** Shared documentation (system
  overview, ADRs) can describe both systems' relationship without being
  split across repositories.
- **Simplified early-stage tooling.** At this stage (single team,
  early-phase project), one repository means one set of CI/CD pipelines,
  one issue tracker context, and one clone to work with.
- **Consistent developer environment setup.** Scripts and infrastructure
  configuration shared between the two systems (e.g., local dev
  orchestration) can live in one place (`scripts/`, `infrastructure/`).
- **Boundaries are enforced by structure, not physical repository
  separation.** Directory-level separation (`backend/`, `frontend/`, `ai/`)
  combined with independent build tooling (Maven for backend, npm/Vite for
  frontend, Python packaging for AI) is sufficient to prevent unwanted
  coupling.

## Consequences

**Positive:**
- Simpler cross-system changes and reviews during early development.
- One place for architecture documentation and ADRs.
- Lower initial tooling and CI/CD overhead.

**Negative / Risks:**
- Risk of accidental coupling between ShopSphere and AI AMS if boundaries
  are not actively maintained (e.g., importing backend code directly into
  AI AMS Python code, or vice versa).
- CI/CD pipelines will need path-based triggers to avoid running unrelated
  builds (e.g., a docs-only change should not rebuild the AI platform).
- As the project and team(s) grow, this decision may need to be revisited
  if independent release cadences or independent access control become
  necessary.

**Mitigations:**
- No cross-imports between `backend/`, `frontend/`, and `ai/` at the code
  level; integration only via REST APIs and domain events.
- Each top-level system retains its own dependency manifest (Maven `pom.xml`
  files, `package.json`, Python `pyproject.toml`/`requirements.txt`) rather
  than a single shared one.
- Revisit this ADR if/when independent deployment cadence, independent
  ownership, or independent access control requirements emerge.

## Alternatives Considered

### Polyrepo (separate repositories for ShopSphere and AI AMS)
- **Pros:** Strongest possible isolation; independent access control and
  release cadence from day one.
- **Cons:** Higher coordination overhead for cross-system changes during a
  phase where the two systems' integration points are still being defined;
  duplicated tooling/CI setup; harder to keep architecture documentation in
  sync across repositories.
- **Rejected for now** because the current project phase benefits more from
  coordinated iteration than from strict repository-level isolation. This
  can be revisited later (see Consequences/Mitigations above).

### Single undivided codebase (no clear module boundaries)
- **Pros:** Simplest possible initial setup.
- **Cons:** High risk of tight coupling between ShopSphere and AI AMS,
  making it difficult to evolve, test, or eventually split the systems.
- **Rejected** because it does not meet the requirement of maintaining
  strong boundaries between the two systems.
