-- V3: Registration captures first and last name (P1-T06).
-- Defaults only backfill any pre-existing rows, then are dropped so new rows must supply values.
ALTER TABLE users ADD COLUMN first_name VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN last_name  VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE users ALTER COLUMN first_name DROP DEFAULT;
ALTER TABLE users ALTER COLUMN last_name  DROP DEFAULT;
