import { useAuth } from '../context/AuthContext.jsx'

export default function Dashboard() {
  const { user } = useAuth()
  return (
    <section className="card">
      <h1>Welcome, {user.firstName}!</h1>
      <p className="muted">You are signed in. Shopping features will appear here in later phases.</p>
      <dl className="profile">
        <dt>Name</dt><dd>{user.firstName} {user.lastName}</dd>
        <dt>Email</dt><dd>{user.email}</dd>
        <dt>Role</dt><dd>{user.role}</dd>
      </dl>
    </section>
  )
}
