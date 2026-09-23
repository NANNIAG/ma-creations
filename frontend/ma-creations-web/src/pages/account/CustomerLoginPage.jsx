import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '../../components/Button/Button'
import { useCustomerAuth } from '../../customer/CustomerAuthContext'
import { ApiError } from '../../services/apiClient'

function normalizeMobileInput(value) {
  return value.replace(/\D/g, '').slice(0, 12)
}

export default function CustomerLoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { requestOtp, verifyOtp, isAuthenticated } = useCustomerAuth()

  const [mobileNumber, setMobileNumber] = useState('')
  const [otp, setOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const redirectTo = location.state?.from || '/account'

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true })
    }
  }, [isAuthenticated, navigate, redirectTo])

  async function handleRequestOtp(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    setBusy(true)
    try {
      const data = await requestOtp(mobileNumber)
      setOtpSent(true)
      setMessage(data?.message || 'OTP sent. Check your phone.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not send OTP')
    } finally {
      setBusy(false)
    }
  }

  async function handleVerifyOtp(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await verifyOtp(mobileNumber, otp)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not verify OTP')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-10 md:px-8">
      <h1 className="text-2xl font-bold">Login</h1>
      <p className="mt-2 text-sm text-ma-muted">
        Sign in with your mobile number. We will send a one-time password (OTP). No password needed.
      </p>

      {!otpSent ? (
        <form onSubmit={handleRequestOtp} className="mt-8 space-y-4">
          <div>
            <label htmlFor="mobile" className="block text-sm font-medium">
              Mobile number
            </label>
            <input
              id="mobile"
              type="tel"
              inputMode="numeric"
              autoComplete="tel"
              value={mobileNumber}
              onChange={(e) => setMobileNumber(normalizeMobileInput(e.target.value))}
              placeholder="10-digit mobile"
              className="mt-1 w-full rounded-lg border border-ma-border bg-ma-surface px-3 py-2.5 text-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary"
              required
            />
          </div>
          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
              {error}
            </p>
          )}
          <Button type="submit" disabled={busy || mobileNumber.length < 10} className="w-full">
            {busy ? 'Sending…' : 'Request OTP'}
          </Button>
        </form>
      ) : (
        <form onSubmit={handleVerifyOtp} className="mt-8 space-y-4">
          <p className="text-sm text-ma-muted">
            OTP sent to <span className="font-medium text-ma-text">{mobileNumber}</span>
          </p>
          <div>
            <label htmlFor="otp" className="block text-sm font-medium">
              Enter OTP
            </label>
            <input
              id="otp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="6-digit OTP"
              className="mt-1 w-full rounded-lg border border-ma-border bg-ma-surface px-3 py-2.5 text-sm tracking-widest focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ma-primary"
              required
            />
          </div>
          {message && (
            <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-800" role="status">
              {message}
            </p>
          )}
          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
              {error}
            </p>
          )}
          <Button type="submit" disabled={busy || otp.length < 4} className="w-full">
            {busy ? 'Verifying…' : 'Verify OTP'}
          </Button>
          <button
            type="button"
            className="w-full text-sm font-medium text-ma-primary underline-offset-2 hover:underline disabled:opacity-50"
            disabled={busy}
            onClick={async () => {
              setError('')
              setBusy(true)
              try {
                const data = await requestOtp(mobileNumber)
                setMessage(data?.message || 'OTP resent.')
                setOtp('')
              } catch (err) {
                setError(err instanceof ApiError ? err.message : 'Could not resend OTP')
              } finally {
                setBusy(false)
              }
            }}
          >
            Resend OTP
          </button>
          <button
            type="button"
            className="w-full text-sm text-ma-muted underline-offset-2 hover:underline"
            disabled={busy}
            onClick={() => {
              setOtpSent(false)
              setOtp('')
              setError('')
              setMessage('')
            }}
          >
            Change mobile number
          </button>
        </form>
      )}

      <p className="mt-8 text-center text-sm text-ma-muted">
        <Link to="/" className="font-medium text-ma-primary hover:underline">
          Continue shopping
        </Link>
      </p>
    </div>
  )
}
