# shopsphere-web

React (JavaScript) + Vite frontend for ShopSphere. Foundation only: public
pages, registration, login and an authenticated dashboard.

## Run

```bash
cp .env.example .env        # then adjust VITE_API_BASE_URL if needed
npm install
npm run dev                 # http://localhost:5173
npm run build               # production build into dist/
npm run preview
```

The backend must be running (`backend/shopsphere-api`, port 8080).

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend origin. The client appends `/api/v1`. Baked in at build time. |

**CORS:** the browser origin of this app must be allowed by the backend. Set
`CORS_ALLOWED_ORIGINS=http://localhost:5173` in the backend environment
(root `.env`). Without it the browser blocks every API call.

## Structure

```
src/
  services/api.js          Axios instance, Bearer interceptor, 401 handling, error messages
  services/authService.js  register / login / fetchCurrentUser
  context/AuthContext.jsx  auth state (user, login, logout), token storage
  routes/ProtectedRoute.jsx  ProtectedRoute + PublicOnlyRoute
  components/              Layout, Header, FormField
  pages/                   Home, Login, Register, Dashboard, NotFound
```

## Authentication and token storage

The backend returns a JWT in the login response body and does not use cookies,
so the token is stored in **`sessionStorage`**. It survives a refresh and is
cleared when the tab closes. Consequence: any injected script (XSS) can read
it, so avoid `dangerouslySetInnerHTML` and untrusted scripts. Passwords are
never stored, and tokens are never logged. Moving to an HttpOnly cookie would
need a backend change and is out of scope.

On startup a stored token is validated with `GET /auth/me`. A 401 on a request
that carried a token clears the session; the next protected navigation sends
the user to `/login`. Authorization is enforced by the backend only; the UI
just hides what the user cannot use.
