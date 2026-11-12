import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Home() {
  const { isAuthenticated } = useAuth()
  return (
    <section className="hero">
      <h1>Welcome to ShopSphere</h1>
      <p>A modern shopping experience, coming together one piece at a time.</p>
      <div className="hero-actions">
        {isAuthenticated ? (
          <Link to="/dashboard" className="btn">Go to dashboard</Link>
        ) : (
          <>
            <Link to="/register" className="btn">Create an account</Link>
            <Link to="/login" className="btn btn-secondary">Log in</Link>
          </>
        )}
      </div>
    </section>
  )
}
