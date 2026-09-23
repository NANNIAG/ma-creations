import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import Button from '../../components/Button/Button'
import { loginAdmin } from '../../services/authService'
import { isAdminAuthenticated } from '../../services/authStorage'
import { ApiError } from '../../services/apiClient'

export default function AdminLoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAdminAuthenticated()) {
    return <Navigate to="/admin" replace />
  }

  function validate() {
    const next = {}
    if (!email.trim()) {
      next.email = 'Email is required'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      next.email = 'Enter a valid email'
    }
    if (!password) {
      next.password = 'Password is required'
    }
    setFieldErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError('')
    if (!validate()) {
      return
    }

    setLoading(true)
    try {
      await loginAdmin({ email: email.trim(), password })
      const redirectTo = location.state?.from || '/admin'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.code === 'INVALID_CREDENTIALS') {
        setFormError('Invalid email or password')
      } else {
        setFormError(err.message || 'Login failed')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-ma-bg px-4">
      <div className="w-full max-w-md rounded-2xl bg-ma-surface p-6 shadow-sm ring-1 ring-ma-border md:p-8">
        <p className="text-sm font-semibold tracking-wide text-ma-primary">MA CREATIONS</p>
        <h1 className="mt-1 text-2xl font-bold">Admin login</h1>
        <p className="mt-2 text-sm text-ma-muted">
          Temporary email login — final identifier shape is CLIENT CONFIRMATION REQUIRED.
        </p>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit} noValidate>
          <label className="block text-sm">
            <span className="font-medium">Email</span>
            <input
              type="email"
              autoComplete="username"
              className="mt-1 w-full rounded-lg border border-ma-border bg-ma-bg px-3 py-2.5"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            {fieldErrors.email && (
              <span className="mt-1 block text-xs text-red-700">{fieldErrors.email}</span>
            )}
          </label>

          <label className="block text-sm">
            <span className="font-medium">Password</span>
            <input
              type="password"
              autoComplete="current-password"
              className="mt-1 w-full rounded-lg border border-ma-border bg-ma-bg px-3 py-2.5"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {fieldErrors.password && (
              <span className="mt-1 block text-xs text-red-700">{fieldErrors.password}</span>
            )}
          </label>

          {formError && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
              {formError}
            </p>
          )}

          <Button type="submit" fullWidth disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>
      </div>
    </div>
  )
}
