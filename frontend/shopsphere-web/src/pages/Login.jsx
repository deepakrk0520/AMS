import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import FormField from '../components/FormField.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { errorMessage } from '../services/api.js'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  async function onSubmit(e) {
    e.preventDefault()
    const next = {}
    if (!form.email.trim()) next.email = 'Email is required'
    if (!form.password) next.password = 'Password is required'
    setErrors(next)
    setFormError('')
    if (Object.keys(next).length) return

    setSubmitting(true)
    try {
      await login({ email: form.email.trim(), password: form.password })
      navigate(location.state?.from || '/dashboard', { replace: true })
    } catch (err) {
      setFormError(
        err.response?.status === 401 ? 'Incorrect email or password.' : errorMessage(err, 'Login failed.'),
      )
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <h1>Log in</h1>
      {location.state?.registered && !formError && (
        <p className="alert alert-success" role="status">Account created. Please log in.</p>
      )}
      {formError && <p className="alert alert-error" role="alert">{formError}</p>}
      <form onSubmit={onSubmit} noValidate>
        <FormField label="Email" name="email" type="email" autoComplete="email"
          value={form.email} onChange={onChange} error={errors.email} />
        <FormField label="Password" name="password" type="password" autoComplete="current-password"
          value={form.password} onChange={onChange} error={errors.password} />
        <button className="btn btn-block" disabled={submitting}>
          {submitting ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="muted">No account? <Link to="/register">Sign up</Link></p>
    </section>
  )
}
