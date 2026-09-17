# ADR-004: Use URI-Based API Versioning

## Status

Accepted

## Context

ShopSphere's backend (and, where it exposes HTTP APIs, AI AMS) will expose
REST APIs consumed by the frontend and, eventually, other internal/external
consumers. As these APIs evolve, breaking changes will eventually be
necessary (field removal, semantic changes, restructuring). A consistent,
project-wide versioning approach is needed so consumers are not broken
unexpectedly and so the migration path for breaking changes is predictable.

## Decision

Use URI-based (path-based) API versioning for all REST APIs.

```
/api/v1/products
/api/v1/orders
/api/v1/customers
```

The version segment (`v1`, `v2`, ...) is incremented when a breaking change
to a resource's contract is introduced. Non-breaking, additive changes
(new optional fields, new endpoints) do not require a version bump.

## Reasons

- **Backward compatibility.** Existing consumers on `/api/v1/...` continue
  working unchanged when a new `/api/v2/...` is introduced for a breaking
  change.
- **Controlled API evolution.** The version boundary makes it explicit
  when and where a breaking change occurs, rather than breaking changes
  landing silently inside an existing contract.
- **Consumer stability.** Frontend, AI AMS, and any other API consumers can
  rely on a given version's contract remaining stable for as long as that
  version is supported.
- **Migration strategy.** Consumers migrate to a new version on their own
  schedule, within a defined deprecation window for the old version, rather
  than being forced to update in lockstep with the API.
- **Simplicity and visibility.** The version is visible directly in the
  URL, in logs, in traces, and in API documentation, making it easy to
  reason about which contract is in use without inspecting headers or
  negotiating content types.

Alternative approaches (header-based versioning, content-type/media-type
versioning) were considered; URI-based versioning was preferred for its
visibility and simplicity, which matters more at this project's current
stage than the header-based approach's advantage of a "cleaner" URL space.

## Consequences

**Benefits:**
- Clear, discoverable versioning directly in the API surface.
- Straightforward routing (a new version can be routed/deployed
  independently of the old one).
- Easy to document and reason about per-version support windows.

**Costs:**
- Multiple versions of a resource may need to be maintained concurrently
  during a migration window, at least until old consumers migrate.
- Care is required to avoid duplicating business logic across versions;
  version-specific concerns should stay at the controller/DTO boundary
  where possible, per [`backend-architecture.md`](../architecture/backend-architecture.md).

This ADR establishes the versioning convention only. No API versioning is
implemented as part of this ticket.
