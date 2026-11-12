import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Header() {
  const { isAuthenticated, user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const close = () => setOpen(false)

  function handleLogout() {
    logout()
    close()
    navigate('/login', { replace: true })
  }

  return (
    <header className="header">
      <div className="header-inner">
        <Link to="/" className="brand" onClick={close}>
          <span className="brand-mark" aria-hidden="true">◎</span> ShopSphere
        </Link>
        <button
          className="nav-toggle"
          aria-label="Toggle navigation"
          aria-expanded={open}
          onClick={() => setOpen((o) => !o)}
        >
          ☰
        </button>
        <nav className={`nav${open ? ' open' : ''}`}>
          <NavLink to="/" end onClick={close}>Home</NavLink>
          {isAuthenticated ? (
            <>
              <NavLink to="/dashboard" onClick={close}>Dashboard</NavLink>
              <span className="nav-user">{user.firstName}</span>
              <button className="btn btn-secondary" onClick={handleLogout}>Log out</button>
            </>
          ) : (
            <>
              <NavLink to="/login" onClick={close}>Log in</NavLink>
              <Link to="/register" className="btn" onClick={close}>Sign up</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
