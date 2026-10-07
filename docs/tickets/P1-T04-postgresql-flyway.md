# P1-T04 - PostgreSQL + Flyway Database Foundation

**Commit:** `8718e11` - `feat: establish postgresql and flyway database foundation`

## Delivered

- `backend/shopsphere-api` Maven project (Spring Boot 3.5.16, Java 21) with
  the Maven wrapper, and `ShopsphereApiApplication`.
- Environment-driven datasource in `application.properties`
  (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`; defaults
  target the local Docker PostgreSQL).
- Flyway (`flyway-core`, `flyway-database-postgresql`) with
  `V1__initial_schema.sql`, which creates no domain tables.
- `DB_*` variables added to `.env.example`; Flyway section added to
  `local-development.md`.

## Rules

- Migrations live in `src/main/resources/db/migration`, named
  `V<n>__<description>.sql`.
- An applied migration is never edited; changes are new migrations.

## Verify

```bash
docker exec shopsphere-postgres psql -U shopsphere -d shopsphere \
  -c "SELECT version, description, success FROM flyway_schema_history;"
```

## Out of scope

Domain schema, entities, REST APIs.
