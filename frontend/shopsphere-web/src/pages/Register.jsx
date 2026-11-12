import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import FormField from '../components/FormField.jsx'
import { errorMessage } from '../services/api.js'
import { register } from '../services/authService.js'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate(f) {
  const e = {}
  if (!f.firstName.trim()) e.firstName = 'First name is required'
  if (!f.lastName.trim()) e.lastName = 'Last name is required'
  if (!f.email.trim()) e.email = 'Email is required'
  else if (!EMAIL_RE.test(f.email.trim())) e.email = 'Enter a valid email address'
  if (!f.password) e.password = 'Password is required'
  else if (f.password.length < 8) e.password = 'Password must be at least 8 characters'
  return e
}

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  async function onSubmit(e) {
    e.preventDefault()
    const next = validate(form)
    setErrors(next)
    setFormError('')
    if (Object.keys(next).length) return

    setSubmitting(true)
    try {
      await register({ ...form, firstName: form.firstName.trim(), lastName: form.lastName.trim(), email: form.email.trim() })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (err) {
      // 400 messages list field names + constraints from the backend; 409 = duplicate email.
      setFormError(errorMessage(err, 'Registration failed.'))
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <h1>Create your account</h1>
      {formError && <p className="alert alert-error" role="alert">{formError}</p>}
      <form onSubmit={onSubmit} noValidate>
        <FormField label="First name" name="firstName" autoComplete="given-name"
          value={form.firstName} onChange={onChange} error={errors.firstName} />
        <FormField label="Last name" name="lastName" autoComplete="family-name"
          value={form.lastName} onChange={onChange} error={errors.lastName} />
        <FormField label="Email" name="email" type="email" autoComplete="email"
          value={form.email} onChange={onChange} error={errors.email} />
        <FormField label="Password" name="password" type="password" autoComplete="new-password"
          value={form.password} onChange={onChange} error={errors.password} />
        <button className="btn btn-block" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Sign up'}
        </button>
      </form>
      <p className="muted">Already registered? <Link to="/login">Log in</Link></p>
    </section>
  )
}
