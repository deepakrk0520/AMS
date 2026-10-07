# P1-T01 - Enterprise Repository & Project Skeleton

**Commit:** `bf8f2cc` - `chore: initialize ShopSphere enterprise monorepo`

## Delivered

- Monorepo layout: `backend/` (`shopsphere-api`, `shopsphere-common`,
  `shopsphere-security`), `frontend/shopsphere-web`, `ai/` (`ams-api`,
  `ams-agents`, `ams-common`), `infrastructure/` (`docker`, `kubernetes`,
  `terraform`), `n8n/workflows`, `scripts`, `docs/`.
- Empty directories reserved with `.gitkeep`.
- Root `README.md`, `.gitignore`, `.editorconfig`.
- `docs/architecture/system-overview.md` and
  [ADR-001](../adr/ADR-001-monorepo.md) (why a monorepo).

## Decisions

- ShopSphere and AI AMS live in one repository but remain independently
  bounded systems (ADR-001).

## Out of scope

No application code, services, or infrastructure.
