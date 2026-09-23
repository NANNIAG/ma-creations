import { Link, useNavigate } from 'react-router-dom'
import Button from '../../components/Button/Button'
import LoadingState from '../../components/Loading/LoadingState'
import { useCustomerAuth } from '../../customer/CustomerAuthContext'

export default function CustomerAccountPage() {
  const navigate = useNavigate()
  const { customer, isAuthenticated, isLoading, logout } = useCustomerAuth()

  if (isLoading) {
    return <LoadingState label="Loading account…" />
  }

  if (!isAuthenticated || !customer) {
    return (
      <div className="mx-auto max-w-lg px-4 py-10 text-center md:px-8">
        <h1 className="text-2xl font-bold">Account</h1>
        <p className="mt-2 text-sm text-ma-muted">Sign in with your mobile number to view your account.</p>
        <Link to="/login" className="mt-6 inline-block">
          <Button>Login</Button>
        </Link>
      </div>
    )
  }

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-10 md:px-8">
      <h1 className="text-2xl font-bold">My account</h1>
      <p className="mt-2 text-sm text-ma-muted">Signed in with mobile OTP.</p>

      <dl className="mt-8 space-y-4 rounded-xl bg-ma-surface p-5 ring-1 ring-ma-border">
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-ma-muted">Mobile</dt>
          <dd className="mt-1 text-base font-medium">{customer.mobileNumber}</dd>
        </div>
        {(customer.firstName || customer.lastName) && (
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-ma-muted">Name</dt>
            <dd className="mt-1 text-base font-medium">
              {[customer.firstName, customer.lastName].filter(Boolean).join(' ')}
            </dd>
          </div>
        )}
        {customer.email && (
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-ma-muted">Email</dt>
            <dd className="mt-1 text-base font-medium">{customer.email}</dd>
          </div>
        )}
      </dl>

      <div className="mt-8 rounded-xl bg-ma-surface p-5 ring-1 ring-ma-border">
        <h2 className="text-base font-semibold text-ma-text">Orders</h2>
        <p className="mt-1 text-sm text-ma-muted">View your order history and details.</p>
        <Link to="/account/orders" className="mt-4 inline-block">
          <Button variant="secondary">View orders</Button>
        </Link>
      </div>

      <p className="mt-4 text-sm text-ma-muted">
        Placed an order as a guest?{' '}
        <Link to="/track-order" className="font-semibold text-ma-primary hover:underline">
          Track with order number
        </Link>
      </p>

      <div className="mt-8 flex flex-wrap gap-3">
        <Button variant="secondary" onClick={handleLogout}>
          Logout
        </Button>
        <Link to="/">
          <Button variant="secondary">Continue shopping</Button>
        </Link>
      </div>
    </div>
  )
}
