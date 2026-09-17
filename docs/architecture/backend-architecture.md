# Backend Architecture

**Status: architectural standard.** This document defines the target
architecture and rules for the ShopSphere Java/Spring Boot backend
(`backend/`). It establishes standards for future implementation tickets.
**No Java code, Spring Boot project, or dependencies are created by this
ticket.**

## Technology baseline

- Java 21
- Spring Boot
- Maven

## Domain module structure

The backend is organized around business domains, not technical layers, at
the top level:

```
backend/shopsphere-api/
  product/
  order/
  inventory/
  customer/
  payment/
```

Each domain is expected to conceptually follow a consistent internal
layering:

```
Controller
    |
    v
Application Service
    |
    v
Domain
    |
    v
Repository
    |
    v
Persistence
```

- **Controller** — HTTP boundary only: request/response mapping, input
  validation, delegation to an application service.
- **Application Service** — orchestrates a use case; coordinates domain
  logic, repositories, and integrations.
- **Domain** — business rules and invariants for the domain, free of
  framework/HTTP/persistence concerns.
- **Repository** — persistence access abstraction for the domain.
- **Persistence** — the actual database mapping/storage concern.

`shopsphere-common` holds cross-domain shared code (e.g., common DTOs,
utilities, error model). `shopsphere-security` holds authentication/
authorization concerns used across domains.

## Engineering rules

1. **Controllers remain thin.** They validate input and delegate; they do
   not contain business logic.
2. **Business logic must not live in controllers.** It belongs in
   application services and the domain layer.
3. **Repositories handle persistence concerns.** No business logic in
   repository implementations.
4. **DTOs should be used at API boundaries.** Requests and responses are
   modeled explicitly, not passed through as internal types.
5. **Database entities should not automatically become API contracts.**
   Entities and DTOs are mapped explicitly; a schema change should not
   silently change the public API.
6. **Business domains should have clear boundaries.** Domains
   (`product`, `order`, `inventory`, `customer`, `payment`) do not reach
   into each other's internals; cross-domain interaction happens through
   well-defined interfaces or events, not direct calls into another
   domain's internal classes.
7. **Avoid giant service classes.** Split by use case/responsibility
   rather than accumulating unrelated methods on one service.
8. **Avoid unnecessary abstractions.** Do not introduce interfaces,
   factories, or layers that have only one implementation and no
   foreseeable need for a second.
9. **Configuration must not be hardcoded.** Environment-specific values
   (URLs, credentials, feature flags, limits) live in externalized
   configuration, not in source code.
10. **Cross-cutting concerns should be handled consistently.** Logging,
    validation, exception handling, and auditing are implemented once
    (e.g., via shared filters/interceptors/aspects in `shopsphere-common`)
    and reused, not reimplemented per domain.
11. **Errors must use a consistent API error model.** All domains return
    errors in the same shape (e.g., code, message, correlation ID) so API
    consumers handle failures uniformly.
12. **APIs must be versioned.** See
    [ADR-004](../adr/ADR-004-api-versioning.md) and
    [`development-standards.md`](development-standards.md) section 12.
13. **External integrations should be isolated behind interfaces/
    adapters** where appropriate, so the domain and application layers do
    not depend directly on a specific external client/SDK.

## Testing expectations

- **Unit tests** — cover domain logic and application services in
  isolation, with dependencies mocked/stubbed. Framework: JUnit + Mockito.
- **Integration tests** — verify a domain's behavior across its real
  layers (controller → service → domain → repository → persistence),
  including actual database interaction where relevant.
- **Testcontainers** — used for integration tests that need a real
  PostgreSQL/Redis/RabbitMQ instance, rather than in-memory substitutes
  that can mask real integration issues.
- **API/integration testing** — controller-level tests validate request
  contracts, status codes, and error responses against the standards in
  [`development-standards.md`](development-standards.md).

No test code, build files, or Java sources are created as part of this
ticket; these are standards for later implementation tickets.
