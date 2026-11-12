import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

/** Requires a signed-in user; otherwise redirects to /login and remembers where they were going. */
export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()
  if (loading) return <p className="status">Loading…</p>
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

/** For /login and /register: signed-in users are sent to the dashboard. */
export function PublicOnlyRoute() {
  const { isAuthenticated, loading } = useAuth()
  if (loading) return <p className="status">Loading…</p>
  if (isAuthenticated) return <Navigate to="/dashboard" replace />
  return <Outlet />
}
