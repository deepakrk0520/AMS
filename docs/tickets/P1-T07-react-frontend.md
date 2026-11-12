# P1-T07 - React Frontend Foundation

**Commit:** `feat: establish react frontend foundation`

## Delivered

- `frontend/shopsphere-web` (existing directory from P1-T01): React 18 +
  Vite, JavaScript, React Router 6, Axios, plain CSS.
- Routes: `/` (public), `/login` and `/register` (public only; signed-in
  users go to `/dashboard`), `/dashboard` (protected), `*` -> 404.
- Header with responsive nav (mobile toggle), logout, ShopSphere branding.
- Registration: first/last name, email, password; client-side checks for
  required fields, email format and 8+ character password; backend 400/409
  messages displayed; success notice on the login page.
- Login: email/password; 401 shows "Incorrect email or password"; redirects
  to the page the user originally requested, else `/dashboard`.
- Centralized Axios client (`services/api.js`): base URL from
  `VITE_API_BASE_URL` + `/api/v1`, Bearer token attached automatically,
  401 on an authenticated request clears the session, consistent
  user-safe error messages (including "cannot reach the server").
- Dashboard shows the profile from `/auth/me` (name, email, role).

## Backend APIs used (`/api/v1`)

`POST /auth/register`, `POST /auth/login`, `GET /auth/me` (P1-T06).
The ticket text listed `/api/auth/*`; the real routes are versioned
(ADR-004), so the client follows the backend.

## Token storage

JWT is returned in the response body (no cookies), so it is kept in
`sessionStorage`; XSS can read it. Documented in
`frontend/shopsphere-web/README.md`. No new auth mechanism was introduced.

## Verify

```bash
docker compose up -d postgres
cd backend/shopsphere-api
set -a; . ../../.env; set +a        # JWT_SECRET must be non-empty
CORS_ALLOWED_ORIGINS=http://localhost:5173 ./mvnw spring-boot:run
# second terminal
cd frontend/shopsphere-web && cp .env.example .env && npm install && npm run dev
```

Result when delivered: `npm run build` succeeds; dev server serves the app;
CORS preflight from `http://localhost:5173` allowed and unknown origins get
403; the auth service code was run against the live backend (register 201,
duplicate 409, invalid input 400, bad login 401, login, `/me`, tampered
token 401 triggers session clear).

## Notes

- Backend code is unchanged. Browser access only needs the existing
  `CORS_ALLOWED_ORIGINS` setting.
- No frontend container or Compose service: Compose currently runs
  infrastructure only (the backend is not containerized either).
- No test framework was added (none existed). The UI was not click-tested in
  a real browser in this ticket.

## Out of scope

Product catalogue, cart, checkout, orders, payments, inventory, admin
workflows, AI/AMS integration, refresh tokens, TypeScript, UI libraries.
