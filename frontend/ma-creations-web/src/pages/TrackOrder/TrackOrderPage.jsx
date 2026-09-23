import { useState } from 'react'
import { Link } from 'react-router-dom'
import Button from '../../components/Button/Button'
import {
  formatInr,
  formatOrderDate,
  OrderStatusBadge,
  PaymentStatusBadge,
  statusLabel,
} from '../../orders/orderDisplay'
import { ApiError } from '../../services/apiClient'
import { trackGuestOrder } from '../../services/guestOrderTrackingService'

export default function TrackOrderPage() {
  const [orderNumber, setOrderNumber] = useState('')
  const [mobileNumber, setMobileNumber] = useState('')
  const [fieldError, setFieldError] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    setFieldError('')
    setError('')
    setResult(null)

    if (!orderNumber.trim()) {
      setFieldError('Order number is required')
      return
    }
    if (!mobileNumber.trim()) {
      setFieldError('Mobile number is required')
      return
    }

    setLoading(true)
    try {
      const data = await trackGuestOrder({
        orderNumber: orderNumber.trim(),
        mobileNumber: mobileNumber.trim(),
      })
      setResult(data)
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setError('Order not found. Check the order number and mobile number used at checkout.')
      } else if (err instanceof ApiError && err.status === 400) {
        setError(err.message || 'Please check the details you entered.')
      } else {
        setError(err.message || 'Could not track order. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-10 md:px-8">
      <p className="text-sm font-semibold uppercase tracking-wide text-ma-primary">MA CREATIONS</p>
      <h1 className="mt-2 font-display text-3xl text-ma-text">Track order</h1>
      <p className="mt-2 text-sm text-ma-muted">
        Enter the order number and the mobile number used at checkout. No account required.
      </p>

      <form onSubmit={handleSubmit} className="mt-8 space-y-4 rounded-xl bg-ma-surface p-5 ring-1 ring-ma-border">
        <label className="block text-sm">
          <span className="font-medium text-ma-text">Order number</span>
          <input
            type="text"
            value={orderNumber}
            onChange={(e) => setOrderNumber(e.target.value)}
            autoComplete="off"
            className="mt-1 w-full rounded-lg border border-ma-border bg-white px-3 py-2.5 text-ma-text outline-none ring-ma-primary focus:ring-2"
            placeholder="e.g. MAC-1001"
          />
        </label>
        <label className="block text-sm">
          <span className="font-medium text-ma-text">Mobile number</span>
          <input
            type="tel"
            inputMode="numeric"
            value={mobileNumber}
            onChange={(e) => setMobileNumber(e.target.value)}
            autoComplete="tel"
            className="mt-1 w-full rounded-lg border border-ma-border bg-white px-3 py-2.5 text-ma-text outline-none ring-ma-primary focus:ring-2"
            placeholder="10-digit mobile"
          />
        </label>
        {fieldError && (
          <p className="text-sm text-red-800" role="alert">
            {fieldError}
          </p>
        )}
        <Button type="submit" fullWidth disabled={loading}>
          {loading ? 'Tracking…' : 'Track order'}
        </Button>
      </form>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
          {error}
        </p>
      )}

      {result && (
        <div className="mt-8 space-y-4 rounded-xl bg-ma-surface p-5 ring-1 ring-ma-border">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-ma-muted">Order</p>
            <p className="mt-1 text-lg font-semibold">{result.orderNumber}</p>
            <p className="mt-1 text-sm text-ma-muted">{formatOrderDate(result.placedAt)}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <OrderStatusBadge status={result.orderStatus} />
            <PaymentStatusBadge status={result.paymentStatus} />
            <span className="inline-flex rounded-md bg-ma-bg px-2 py-0.5 text-xs font-medium ring-1 ring-ma-border">
              {statusLabel(result.paymentMethod)}
            </span>
          </div>

          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-ma-muted">
              Shipment tracking
            </p>
            {result.trackingNumber ? (
              <p className="mt-1 font-semibold text-ma-text">{result.trackingNumber}</p>
            ) : (
              <p className="mt-1 text-sm text-ma-muted">
                Tracking information will be available after the order is shipped.
              </p>
            )}
          </div>

          <ul className="divide-y divide-ma-border border-t border-ma-border pt-2">
            {(result.items || []).map((item, index) => (
              <li key={`${item.title}-${index}`} className="flex justify-between gap-3 py-2 text-sm">
                <span>
                  {item.title} × {item.quantity}
                </span>
                <span className="font-medium">{formatInr(item.lineSubtotal)}</span>
              </li>
            ))}
          </ul>

          <dl className="space-y-1 border-t border-ma-border pt-3 text-sm">
            {(result.deliveryCity || result.deliveryPostalCode) && (
              <div className="flex justify-between gap-4 text-ma-muted">
                <dt>Delivery area</dt>
                <dd>
                  {[result.deliveryCity, result.deliveryPostalCode].filter(Boolean).join(' · ')}
                </dd>
              </div>
            )}
            <div className="flex justify-between gap-4 font-semibold text-ma-text">
              <dt>Grand total</dt>
              <dd>{formatInr(result.grandTotal)}</dd>
            </div>
          </dl>
        </div>
      )}

      <p className="mt-8 text-center text-sm text-ma-muted">
        Have an account?{' '}
        <Link to="/login" className="font-semibold text-ma-primary hover:underline">
          Log in
        </Link>{' '}
        to view full order history.
      </p>
    </div>
  )
}
