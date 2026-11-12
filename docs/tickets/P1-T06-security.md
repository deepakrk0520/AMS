# P1-T06 - Security

**Commit:** `b0ada66` - `feat: establish spring security and jwt authentication`
(registration, user names and `/admin/test` were added afterwards; see the
README for the follow-up commit)

## Delivered

- Spring Security, stateless (no sessions, CSRF off, no form/basic login).
- BCrypt password hashing; passwords are never stored or returned in plain
  text.
- JWT (HS256) issued on login with `sub` = email, `roles`, `iss`, `iat`,
  `exp`. Validation uses Spring's resource-server filter: signature,
  expiry and issuer are checked, and other algorithms are rejected.
- Roles `CUSTOMER` and `ADMIN`. `/api/v1/admin/**` requires `ADMIN`; every
  other route requires a valid token unless listed as public.
- Endpoints (under `/api/v1`, per ADR-004):

  | Method | Path | Access |
  |---|---|---|
  | POST | `/auth/register` | public, returns 201, always creates a CUSTOMER |
  | POST | `/auth/login` | public, returns token + user |
  | GET | `/auth/me` | authenticated |
  | GET | `/admin/test` | ADMIN |

- Errors use the common `ApiError` body: 400 invalid input (field names
  only, never rejected values), 401 bad credentials / missing, invalid or
  expired token, 403 wrong role, 409 duplicate email (including a
  concurrent-registration race).
- Flyway: `V2__create_users.sql` (users table), `V3__add_user_names.sql`
  (first/last name).
- Config: `JWT_SECRET` (required, at least 32 characters, no default; the app
  refuses to start without it), `JWT_EXPIRATION` (default `15m`),
  `JWT_ISSUER`, `CORS_ALLOWED_ORIGINS` (deny by default). All documented in
  `.env.example`.
- Tests: registration, duplicate email, no self-assigned admin, input
  validation, BCrypt hashing, login success/failure, JWT validity, expired /
  forged / wrong-issuer tokens, no token, CUSTOMER vs ADMIN access.

## Verify

```bash
docker compose up -d postgres
cd backend/shopsphere-api
set -a; . ../../.env; set +a        # JWT_SECRET must be set
./mvnw clean verify
./mvnw spring-boot:run
B=localhost:8080/api/v1
curl -X POST $B/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"L","email":"ada@example.com","password":"s3cret-pass"}'
curl -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"s3cret-pass"}'
curl $B/auth/me -H "Authorization: Bearer <accessToken>"
curl $B/admin/test -H "Authorization: Bearer <accessToken>"   # 403 for a customer
```

Result when delivered: 24 tests passed; live run confirmed 201 / 409 / 401 /
200 / 401 (no token) / 403 (customer on admin) / 200 (admin).

## Notes

- JWT filter is Spring's built-in bearer-token filter, not a custom one.
- Public registration cannot create admins. There is no admin bootstrap yet;
  promote a user in the database (`UPDATE users SET role='ADMIN' ...`).
- Access tokens only; no refresh tokens, logout or revocation.

## Out of scope

Product, cart, order and inventory logic, refresh tokens, password reset,
email verification, admin management API, AI AMS integration, frontend.
